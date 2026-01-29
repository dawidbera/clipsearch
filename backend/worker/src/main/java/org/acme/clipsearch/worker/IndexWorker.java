package org.acme.clipsearch.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.tika.TikaParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.clipsearch.model.SqsEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@ApplicationScoped
public class IndexWorker {

    @Inject
    S3Client s3;

    @Inject
    SqsClient sqs;

    @Inject
    RestClient esClient;

    @Inject
    ObjectMapper mapper;

    @Inject
    TikaParser tika;

    @ConfigProperty(name = "clipsearch.sqs.queue")
    String queueName;

    @ConfigProperty(name = "clipsearch.es.index", defaultValue = "clipsearch-uploads")
    String esIndex;

    @Scheduled(every = "5s")
    void poll() {
        try {
            String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
            
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(5)
                    .waitTimeSeconds(5)
                    .build();

            List<Message> messages = sqs.receiveMessage(receiveRequest).messages();
            
            for (Message message : messages) {
                processMessage(queueUrl, message);
            }
        } catch (Exception e) {
            log.error("Error polling SQS: {}", e.getMessage());
        }
    }

    private void processMessage(String queueUrl, Message message) {
        try {
            SqsEvent event = mapper.readValue(message.body(), SqsEvent.class);
            log.info("Processing file: {}/{}", event.getBucket(), event.getKey());

            // 1. Get from S3
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(event.getBucket())
                    .key(event.getKey())
                    .build();

            byte[] contentBytes;
            try (ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(getRequest)) {
                contentBytes = s3Stream.readAllBytes();
            }

            // 2. Calculate SHA256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contentBytes);
            String sha256 = HexFormat.of().formatHex(hash);

            // 3. Prepare ES Document
            ObjectNode doc = mapper.createObjectNode();
            doc.put("uploadId", event.getUploadId());
            doc.put("filename", event.getFilename());
            doc.put("contentType", event.getContentType());
            doc.put("sizeBytes", contentBytes.length);
            doc.put("sha256", sha256);
            doc.put("uploadedAt", event.getUploadedAt());
            doc.put("bucket", event.getBucket());
            doc.put("key", event.getKey());
            doc.put("ingestedAt", Instant.now().toString());

            var tagsArray = doc.putArray("tags");
            if (event.getTags() != null) {
                event.getTags().forEach(tagsArray::add);
            }

            // 4. Content extraction (if text/plain or application/pdf)
            String extractedContent = null;
            if ("text/plain".equals(event.getContentType())) {
                extractedContent = new String(contentBytes, StandardCharsets.UTF_8);
            } else if ("application/pdf".equals(event.getContentType())) {
                try {
                    extractedContent = tika.getText(new ByteArrayInputStream(contentBytes));
                    log.info("Extracted {} chars from PDF", extractedContent != null ? extractedContent.length() : 0);
                } catch (Exception e) {
                    log.warn("Failed to extract text from PDF {}: {}", event.getKey(), e.getMessage());
                }
            }

            if (extractedContent != null) {
                // Limit to 256KB as per spec
                if (extractedContent.length() > 256 * 1024) {
                    extractedContent = extractedContent.substring(0, 256 * 1024);
                }
                doc.put("content", extractedContent);
            }

            // 5. Index to ES
            // Using uploadId as _id to ensure every upload is a separate document in ES
            String docId = event.getUploadId();
            
            Request esRequest = new Request("PUT", "/" + esIndex + "/_doc/" + docId);
            esRequest.setJsonEntity(mapper.writeValueAsString(doc));
            esClient.performRequest(esRequest);

            log.info("Successfully indexed document: {}", docId);

            // 6. Delete from SQS
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

        } catch (Exception e) {
            log.error("Failed to process message {}: {}", message.messageId(), e.getMessage(), e);
        }
    }
}
