package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.qa.dto.ConversationCreateDTO;
import com.xxr.qa.dto.QaAskDTO;

public interface QaService {
    ResponseResult createConversation(Long userId, ConversationCreateDTO dto);
    ResponseResult ask(Long userId, QaAskDTO dto);
    ResponseResult getConversations(Long userId, int page, int pageSize, String keyword);
    ResponseResult getConversationDetail(Long userId, Long conversationId);
    ResponseResult feedback(Long userId, Long messageId, int score);
}