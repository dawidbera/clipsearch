package org.acme.clipsearch.worker;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.acme.clipsearch.model.SqsEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for the IndexWorker class.
 * Tests the message polling and processing workflow with mocked AWS services.
 */
@QuarkusTest
public class IndexWorkerTest {

    @InjectMock
    S3Client s3;

    @InjectMock
    SqsClient sqs;

    @Inject
    IndexWorker worker;

    /**
     * Test the poll and process workflow.
     * Verifies that the IndexWorker correctly retrieves messages from SQS,
     * fetches files from S3, and processes them for indexing.
     * 
     * This test mocks the entire flow:
     * 1. SQS returns an upload event message
     * 2. S3 returns the file content
     * 3. Worker processes the message and index the content
     * 4. Verifies that S3 getObject was called at least once
     */
    @Test
    public void testPollAndProcess() {
        // Mock SQS - Configure the queue URL retrieval
        Mockito.when(sqs.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://test").build());
        
        // Create a test SQS message with upload event details
        Message msg = Message.builder()
                .body("{\"uploadId\":\"123\",\"bucket\":\"b\",\"key\":\"k\",\"filename\":\"f.txt\",\"contentType\":\"text/plain\"}")
                .receiptHandle("rh")
                .build();
        
        // Mock SQS receiveMessage to return one message, then empty list on subsequent calls        
        Mockito.when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(msg).build())
                .thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

        // Mock S3 - Create test file content and stream
        byte[] content = "Hello world".getBytes();
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(content)));
        
        // Mock S3 getObject to return the test file stream
        Mockito.when(s3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        // Execute the poll operation - this will retrieve and process the message
        worker.poll();

        // Verify that S3 getObject was called at least once to fetch the file
        verify(s3, atLeastOnce()).getObject(any(GetObjectRequest.class));
    }
}
