package com.xxr.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag.chat")
@Data
public class ChatProperties {
    private String provider;
    private String model = "qwen-plus";
    private String apiKey;
    private String apiUrl;
}