package com.xxr.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag.qdrant")
@Data
public class QdrantProperties {
    private String host = "localhost";
    private int port = 6334;
    private int httpPort = 6333;
    private boolean useTls = false;
    private int dimension = 1024;
}