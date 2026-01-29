package org.acme.clipsearch.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.clipsearch.model.SqsEvent;
import org.acme.clipsearch.model.UploadResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/uploads")
public class UploadResource {

    @Inject
    S3Client s3;

    @Inject
    SqsClient sqs;

    @Inject
    RestClient restClient;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "clipsearch.s3.bucket")
    String bucketName;

    @ConfigProperty(name = "clipsearch.sqs.queue")
    String queueName;

    private static final String INDEX = "clipsearch-uploads";

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public UploadResponse upload(@RestForm("file") FileUpload file,
                                 @RestForm("tags") String tagsRaw) throws Exception {
        // ... (existing code)

        String uploadId = UUID.randomUUID().toString();
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        // Date path: YYYY/MM/DD
        String datePath = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());

        String key = "uploads/" + datePath + "/" + uploadId + "-" + file.fileName();
        
        List<String> tagList = (tagsRaw == null || tagsRaw.isBlank()) 
                ? Collections.emptyList() 
                : Arrays.stream(tagsRaw.split(",")).map(String::trim).collect(Collectors.toList());

        // 1. Upload to S3
        s3.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.contentType())
                .build(), RequestBody.fromFile(file.uploadedFile()));

        // 2. Prepare Event
        SqsEvent event = SqsEvent.builder()
                .uploadId(uploadId)
                .bucket(bucketName)
                .key(key)
                .filename(file.fileName())
                .contentType(file.contentType())
                .uploadedAt(now)
                .tags(tagList)
                .build();

        // 3. Send to SQS
        String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(mapper.writeValueAsString(event))
                .build());

        // 4. Return Response
        return UploadResponse.builder()
                .uploadId(uploadId)
                .bucket(bucketName)
                .key(key)
                .filename(file.fileName())
                .contentType(file.contentType())
                .uploadedAt(now)
                .tags(tagList)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode listUploads(@QueryParam("limit") @DefaultValue("50") int limit) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("size", limit);
        root.putObject("query").putObject("match_all");
        root.putArray("sort").addObject().putObject("uploadedAt").put("order", "desc");

        return executeSearch(root);
    }

    private JsonNode executeSearch(ObjectNode dsl) throws IOException {
        Request request = new Request("GET", "/" + INDEX + "/_search");
        request.setJsonEntity(mapper.writeValueAsString(dsl));

        try {
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode esResponse = mapper.readTree(responseBody);

            ObjectNode result = mapper.createObjectNode();
            ArrayNode items = result.putArray("items");

            JsonNode hits = esResponse.path("hits").path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    JsonNode source = hit.path("_source");
                    ObjectNode item = items.addObject();
                    item.put("id", hit.path("_id").asText());
                    item.setAll((ObjectNode) source);
                }
            }

            result.put("total", esResponse.path("hits").path("total").path("value").asLong());
            return result;
        } catch (Exception e) {
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
