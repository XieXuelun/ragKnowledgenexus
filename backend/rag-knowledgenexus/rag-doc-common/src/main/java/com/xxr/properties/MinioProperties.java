package com.xxr.properties;

import io.minio.messages.Bucket;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag.minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Bucket bucket;
    @Data
    public static class Bucket {
        private String document;
        private String avatar;
    }

}