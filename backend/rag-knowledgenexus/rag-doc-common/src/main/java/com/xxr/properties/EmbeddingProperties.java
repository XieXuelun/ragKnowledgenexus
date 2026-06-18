package com.xxr.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding 向量化配置
 * 对应 application.yml 中的 rag.embedding.*
 */
@Component
@ConfigurationProperties(prefix = "rag.embedding")
@Data
public class EmbeddingProperties {
    /** 向量化提供商：aliyun / openai */
    private String provider;
    /** 向量模型名称 */
    private String model = "text-embedding-v3";
    /** API Key */
    private String apiKey;
    /** API URL */
    private String apiUrl;
}
