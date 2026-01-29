package org.acme.clipsearch.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    RestClient restClient;

    @Inject
    ObjectMapper mapper;

    private static final String INDEX = "clipsearch-uploads";

    @GET
    public JsonNode search(@QueryParam("q") String q,
                           @QueryParam("contentType") String contentType,
                           @QueryParam("tag") String tag,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("10") int size) throws IOException {

        // Build DSL Query
        ObjectNode root = mapper.createObjectNode();
        root.put("from", page * size);
        root.put("size", size);

        ObjectNode query = root.putObject("query");
        ObjectNode bool = query.putObject("bool");
        ArrayNode must = bool.putArray("must");

        // Full text search
        if (q != null && !q.isBlank()) {
            ObjectNode multiMatch = must.addObject().putObject("multi_match");
            multiMatch.put("query", q);
            multiMatch.putArray("fields").add("filename").add("content");
        } else {
            must.addObject().putObject("match_all");
        }

        // Filters
        if (contentType != null && !contentType.isBlank()) {
            ObjectNode term = must.addObject().putObject("term");
            term.putObject("contentType").put("value", contentType);
        }
        if (tag != null && !tag.isBlank()) {
            ObjectNode term = must.addObject().putObject("term");
            term.putObject("tags").put("value", tag);
        }

        // Execute
        return executeSearch(root);
    }

    private JsonNode executeSearch(ObjectNode dsl) throws IOException {
        Request request = new Request("GET", "/" + INDEX + "/_search");
        request.setJsonEntity(mapper.writeValueAsString(dsl));

        try {
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode esResponse = mapper.readTree(responseBody);

            // Transform ES response to Frontend Contract
            ObjectNode result = mapper.createObjectNode();
            ArrayNode items = result.putArray("items");

            JsonNode hits = esResponse.path("hits").path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    JsonNode source = hit.path("_source");
                    ObjectNode item = items.addObject();
                    item.put("id", hit.path("_id").asText());
                    // Copy fields
                    item.setAll((ObjectNode) source);
                }
            }
            
            result.put("total", esResponse.path("hits").path("total").path("value").asLong());
            return result;
        } catch (Exception e) {
             // If index doesn't exist yet, return empty
            if (e.getMessage().contains("index_not_found_exception")) {
                ObjectNode empty = mapper.createObjectNode();
                empty.putArray("items");
                empty.put("total", 0);
                return empty;
            }
            throw e;
        }
    }
}
