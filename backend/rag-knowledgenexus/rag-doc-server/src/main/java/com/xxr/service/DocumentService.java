package com.xxr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.document.dtos.DocumentQueryDto;
import com.xxr.document.pojo.DocDocument;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService extends IService<DocDocument> {
    /**
     * 获取知识库中的文档列表
     * @param documentQueryDto
     * @return
     */
    ResponseResult getList(DocumentQueryDto documentQueryDto);

    /**
     * 上传知识库的文档
     * @param file
     * @return
     */
    ResponseResult uploadDocument(MultipartFile file, Long kbId) throws Exception;

    /**
     * 查询文档详情
     * @param id
     * @return
     */
    ResponseResult getDocumentDetail(Long id);

    /**
     * 获取文档预览地址
     * @param docId
     * @param expireSeconds
     * @return
     */
    ResponseResult getDocumentPreviewUrl(Long docId, Integer expireSeconds) throws Exception;

    /**
     * 更新文档标题
     * @param docId
     * @param fileName
     * @return
     */
    ResponseResult updateDocumentTitle(Long docId, String fileName);

    /**
     * 删除文档信息，逻辑删除
     * @param docId
     * @return
     */
    ResponseResult deleteDocument(Long docId);

    /**
     * 重新解析文档（删除旧分片，重新解析）
     * @param docId 文档ID
     * @return 解析结果
     */
    ResponseResult reparseDocument(Long docId);

    /**
     * 根据知识库id查询文档
     * @param kbId
     * @return
     */
    ResponseResult selectbyKbId(Long kbId);
}
