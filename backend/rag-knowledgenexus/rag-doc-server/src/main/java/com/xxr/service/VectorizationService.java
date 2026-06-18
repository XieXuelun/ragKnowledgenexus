package com.xxr.service;

/**
 * 向量化编排服务。
 * 负责：读取待处理 chunk -> 调用 Embedding -> 写入 Lucene -> 更新 MySQL 状态。
 */
public interface VectorizationService {

    /** 将指定文档的所有待向量化 chunk 完成处理 */
    void vectorizeByDocId(Long docId);

}
