package com.xxr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.kb.dtos.DocImportDTO;
import com.xxr.kb.dtos.KnowledgeBaseQueryDTO;
import com.xxr.kb.dtos.KnowledgeBaseRequest;
import com.xxr.kb.pojo.DocKnowledgeBase;

public interface KnowledgeService extends IService<DocKnowledgeBase> {
    /**
     * 获取知识库列表
     * @param knowledgeBaseQueryDTO
     * @return
     */
    ResponseResult getList(KnowledgeBaseQueryDTO knowledgeBaseQueryDTO);

    /**
     * c查询知识库详情
     * @param id
     * @return
     */
    ResponseResult selectKnowledge(Long id);

    /**
     * 创建个人知识库
     * @param knowledgeBaseRequest
     * @return
     */
    ResponseResult createKnowledge(KnowledgeBaseRequest knowledgeBaseRequest);

    /**
     * 修改知识库
     * @param knowledgeBaseRequest
     * @return
     */
    ResponseResult updateKnowledge(KnowledgeBaseRequest knowledgeBaseRequest);

    /**
     * 收藏/取消收藏知识库
     * @param kbId
     * @return
     */
    ResponseResult favoriteKnowledge(Long kbId);

    /**
     * 删除自有知识库
     * @param kbId
     * @return
     */
    ResponseResult deleteKnowledge(Long kbId);

    /**
     * 查询知识库名称
     * @return
     */
    ResponseResult selectKnowledgeName();
}
