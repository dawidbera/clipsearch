package org.acme.clipsearch.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.time.Duration;
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
    S3Presigner s3Presigner;

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
        String uploadId = UUID.randomUUID().toString();
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String datePath = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());

        String key = "uploads/" + datePath + "/" + uploadId + "-" + file.fileName();
        
        List<String> tagList = (tagsRaw == null || tagsRaw.isBlank()) 
                ? Collections.emptyList() 
                : Arrays.stream(tagsRaw.split(",")).map(String::trim).collect(Collectors.toList());

        s3.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.contentType())
                .build(), RequestBody.fromFile(file.uploadedFile()));

        SqsEvent event = SqsEvent.builder()
                .uploadId(uploadId)
                .bucket(bucketName)
                .key(key)
                .filename(file.fileName())
                .contentType(file.contentType())
                .uploadedAt(now)
                .tags(tagList)
                .build();

        String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(mapper.writeValueAsString(event))
                .build());

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
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode getDownloadUrl(@PathParam("id") String id) throws IOException {
        // 1. Find in ES to get bucket and key
        Request esRequest = new Request("GET", "/" + INDEX + "/_doc/" + id);
        Response response = restClient.performRequest(esRequest);
        JsonNode doc = mapper.readTree(EntityUtils.toString(response.getEntity()));
        JsonNode source = doc.path("_source");

        String bucket = source.path("bucket").asText();
        String key = source.path("key").asText();

        // 2. Generate Presigned URL
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();

        // Local development hack: if URL contains 'localstack', replace it with 'localhost' 
        // so the host browser can reach it.
        if (url.contains("http://localstack:")) {
            url = url.replace("http://localstack:", "http://localhost:");
        }

        ObjectNode result = mapper.createObjectNode();
        result.put("url", url);
        return result;
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

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public jakarta.ws.rs.core.Response delete(@PathParam("id") String id) throws IOException {
        // 1. Find in ES to get bucket and key
        Request esGetRequest = new Request("GET", "/" + INDEX + "/_doc/" + id);
        try {
            Response esResponse = restClient.performRequest(esGetRequest);
            JsonNode doc = mapper.readTree(EntityUtils.toString(esResponse.getEntity()));
            JsonNode source = doc.path("_source");

            String bucket = source.path("bucket").asText();
            String key = source.path("key").asText();

            // 2. Delete from S3
            s3.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            // 3. Delete from ES
            Request esDeleteRequest = new Request("DELETE", "/" + INDEX + "/_doc/" + id);
            restClient.performRequest(esDeleteRequest);

            // 4. Force Index Refresh (important for near-real-time search)
            Request refreshRequest = new Request("POST", "/" + INDEX + "/_refresh");
            restClient.performRequest(refreshRequest);

            return jakarta.ws.rs.core.Response.noContent().build();
        } catch (Exception e) {
            if (e.getMessage().contains("404")) {
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).build();
            }
            throw e;
        }
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