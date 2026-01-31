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

@QuarkusTest
public class IndexWorkerTest {

    @InjectMock
    S3Client s3;

    @InjectMock
    SqsClient sqs;

    @Inject
    IndexWorker worker;

    @Test
    public void testPollAndProcess() {
        // Mock SQS
        Mockito.when(sqs.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://test").build());
        
        Message msg = Message.builder()
                .body("{\"uploadId\":\"123\",\"bucket\":\"b\",\"key\":\"k\",\"filename\":\"f.txt\",\"contentType\":\"text/plain\"}")
                .receiptHandle("rh")
                .build();
                
        Mockito.when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(msg).build())
                .thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

        // Mock S3
        byte[] content = "Hello world".getBytes();
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(content)));
        
        Mockito.when(s3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        // Run poll
        worker.poll();

        // Verify s3.getObject was called
        verify(s3, atLeastOnce()).getObject(any(GetObjectRequest.class));
    }
}
