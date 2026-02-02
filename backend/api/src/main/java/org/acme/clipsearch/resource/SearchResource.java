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

/**
 * REST API Resource for searching indexed documents in Elasticsearch.
 * Provides full-text search, filtering by content type and tags, and pagination.
 */
@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    RestClient restClient;

    @Inject
    ObjectMapper mapper;

    private static final String INDEX = "clipsearch-uploads";

    /**
     * Executes a search against the Elasticsearch index.
     * 
     * @param q The search query string for full-text search.
     * @param contentType Filter by MIME type (optional).
     * @param tag Filter by a specific tag (optional).
     * @param page Zero-based page index for pagination.
     * @param size Number of results per page.
     * @return A JSON object containing a list of matching items and the total hit count.
     */
    @GET
    public JsonNode search(@QueryParam("q") String q,
                           @QueryParam("contentType") String contentType,
                           @QueryParam("tag") String tag,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("10") int size) throws IOException {

        // Build the Elasticsearch Query Domain Specific Language (DSL)
        ObjectNode root = mapper.createObjectNode();
        root.put("from", page * size);
        root.put("size", size);

        ObjectNode query = root.putObject("query");
        ObjectNode bool = query.putObject("bool");
        ArrayNode must = bool.putArray("must");

        // Full text search: matches both filename and extracted content
        if (q != null && !q.isBlank()) {
            ObjectNode multiMatch = must.addObject().putObject("multi_match");
            multiMatch.put("query", q);
            multiMatch.putArray("fields").add("filename").add("content");
        } else {
            // Default to matching everything if no query is provided
            must.addObject().putObject("match_all");
        }

        // Apply filters: these use 'term' queries for exact matching
        if (contentType != null && !contentType.isBlank()) {
            ObjectNode term = must.addObject().putObject("term");
            term.putObject("contentType").put("value", contentType);
        }
        if (tag != null && !tag.isBlank()) {
            ObjectNode term = must.addObject().putObject("term");
            term.putObject("tags").put("value", tag);
        }

        // Configure highlighting to show snippets of matching text in the search results
        ObjectNode highlight = root.putObject("highlight");
        highlight.put("pre_tags", "<em>").put("post_tags", "</em>");
        ObjectNode fields = highlight.putObject("fields");
        // We want highlight fragments from the content field
        fields.putObject("content").put("number_of_fragments", 3).put("fragment_size", 150);
        fields.putObject("filename");

        return executeSearch(root);
    }

    /**
     * Sends the DSL query to Elasticsearch and transforms the response into a frontend-friendly format.
     *
     * @param dsl The Elasticsearch Query DSL as a JSON object.
     * @return A JSON object with "items" and "total" fields.
     * @throws IOException If the request fails.
     */
    private JsonNode executeSearch(ObjectNode dsl) throws IOException {
        Request request = new Request("GET", "/" + INDEX + "/_search");
        request.setJsonEntity(mapper.writeValueAsString(dsl));

        try {
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode esResponse = mapper.readTree(responseBody);

            // Transform ES response to Frontend Contract (mapping _source and highlight fields)
            ObjectNode result = mapper.createObjectNode();
            ArrayNode items = result.putArray("items");

            JsonNode hits = esResponse.path("hits").path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    JsonNode source = hit.path("_source");
                    ObjectNode item = items.addObject();
                    item.put("id", hit.path("_id").asText());
                    
                    // Copy all fields from the original document
                    item.setAll((ObjectNode) source);
                    
                    // Attach highlights if present in the ES response
                    JsonNode highlight = hit.path("highlight");
                    if (!highlight.isMissingNode()) {
                        ObjectNode highlightObj = item.putObject("highlights");
                        highlight.fields().forEachRemaining(entry -> {
                            highlightObj.set(entry.getKey(), entry.getValue());
                        });
                    }
                }
            }
            
            result.put("total", esResponse.path("hits").path("total").path("value").asLong());
            return result;
        } catch (Exception e) {
             // Gracefully handle cases where the index doesn't exist yet (e.g. before any uploads)
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
