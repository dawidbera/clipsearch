package org.acme.clipsearch.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SqsEvent {
    private String uploadId;
    private String bucket;
    private String key;
    private String filename;
    private String contentType;
    private String uploadedAt;
    private List<String> tags;
}
