package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.document.pojo.DocDocument;

/**
 * 文档解析服务接口
 */
public interface DocumentParseService {

    /**
     * 异步解析文档
     * @param document 文档
     */
    void parseDocumentAsync(DocDocument document);


    void reparseDocument(DocDocument docDocument);
}
