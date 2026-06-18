package com.xxr.service;

import java.util.List;

/**
 * Embedding 向量化服务接口。
 */
public interface EmbeddingService {

    /** 单条文本向量化 */
    float[] embed(String text);

    /** 批量文本向量化，输入输出顺序严格对应 */
    List<float[]> embedBatch(List<String> texts);

    /** 返回向量维度 */
    int dimension();
}
