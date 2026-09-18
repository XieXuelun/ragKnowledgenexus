package com.xxr.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxr.common.dtos.PageResponseResult;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.document.pojo.DocChunk;
import com.xxr.mapper.ChunkMapper;
import com.xxr.mapper.QaConversationMapper;
import com.xxr.mapper.QaMessageMapper;
import com.xxr.qa.dto.ChatResult;
import com.xxr.qa.dto.ConversationCreateDTO;
import com.xxr.qa.dto.QaAskDTO;
import com.xxr.qa.pojo.QaConversation;
import com.xxr.qa.pojo.QaMessage;
import com.xxr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    private final QaConversationMapper conversationMapper;
    private final QaMessageMapper messageMapper;
    private final ChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final QdrantIndexService qdrantIndexService;
    private final ChatService chatService;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的知识库问答助手。请根据提供的文档内容回答用户的问题。
            要求：
            1. 只基于提供的文档内容回答，不要编造信息。
            2. 如果文档内容不足以回答问题，请明确说明。
            3. 回答要简洁、准确、有条理。
            4. 如果文档内容包含多个相关点，请分点说明。
            """;

    @Override
    public ResponseResult createConversation(Long userId, ConversationCreateDTO dto) {
        QaConversation conv = new QaConversation();
        conv.setUserId(userId);
        conv.setKbId(dto.getKbId());
        conv.setCreateTime(LocalDateTime.now());
        conv.setIsDeleted(DeleteConstants.NOT_DELETED);
        conv.setTitle(dto.getTitle() != null ? dto.getTitle() : "新对话");
        conversationMapper.insert(conv);
        return ResponseResult.okResult(Map.of("conversationId", conv.getId(), "title", conv.getTitle()));
    }

    @Override
    public ResponseResult ask(Long userId, QaAskDTO dto) {
        QaConversation conv = conversationMapper.selectById(dto.getConversationId());
        if (conv == null) {
            return ResponseResult.errorResult(400, "对话不存在");
        }
        Long kbId = conv.getKbId();
        int topK = dto.getTopK() != null ? dto.getTopK() : 5;

        // 1. 问题向量化
        float[] queryVector = embeddingService.embed(dto.getQuestion());
        if (queryVector.length == 0) {
            return ResponseResult.errorResult(500, "问题向量化失败");
        }

        // 2. Qdrant 检索
        List<QdrantIndexService.SearchResult> searchResults = qdrantIndexService.search(kbId, queryVector, topK);
        if (searchResults.isEmpty()) {
            QaMessage msg = saveMessage(userId, conv.getId(), dto.getQuestion(), "未找到相关文档内容，无法回答该问题。", "[]", null, dto.getSearchType());
            return ResponseResult.okResult(Map.of("messageId", msg.getId(), "answer", msg.getAnswer(), "sources", List.of()));
        }

        // 3. 获取检索到的 chunk 内容
        List<Long> chunkIds = searchResults.stream().map(QdrantIndexService.SearchResult::getChunkId).collect(Collectors.toList());
        List<DocChunk> chunks = chunkMapper.selectBatchIds(chunkIds);
        Map<Long, Float> scoreMap = searchResults.stream().collect(Collectors.toMap(QdrantIndexService.SearchResult::getChunkId, QdrantIndexService.SearchResult::getScore));

        // 构建上下文
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocChunk c = chunks.get(i);
            context.append("【文档片段").append(i + 1).append("】\n").append(c.getContent()).append("\n\n");
        }

        // 4. LLM 生成回答
        ChatResult chatResult = chatService.chat
                (SYSTEM_PROMPT, "参考资料：\n" + context + "\n用户问题：" + dto.getQuestion());
        String answer = chatResult.getContent();

        // 5. 构建 sources
        List<Map<String, Object>> sources = new ArrayList<>();
        for (DocChunk c : chunks) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("chunkId", c.getId());
            s.put("content", c.getContent().length() > 200 ? c.getContent().substring(0, 200) + "..." : c.getContent());
            s.put("score", scoreMap.getOrDefault(c.getId(), 0f));
            sources.add(s);
        }

        // 6. 保存消息
        QaMessage msg = saveMessage
                (userId, conv.getId(), dto.getQuestion(), answer, JSON.toJSONString(sources), chatResult, dto.getSearchType());

        return ResponseResult.okResult(Map.of("messageId", msg.getId(), "answer", answer, "sources", sources));
    }

    @Override
    public ResponseResult getConversations(Long userId, int page, int pageSize, String keyword) {
        LambdaQueryWrapper<QaConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QaConversation::getUserId, userId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(QaConversation::getTitle, keyword);
        }
        wrapper.orderByDesc(QaConversation::getCreateTime);
        IPage<QaConversation> result = conversationMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResponseResult(page, pageSize, (int) result.getTotal()) {{
            setData(result.getRecords());
        }};
    }

    @Override
    public ResponseResult getConversationDetail(Long userId, Long conversationId) {
        QaConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            return ResponseResult.errorResult(400, "对话不存在");
        }
        LambdaQueryWrapper<QaMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QaMessage::getConversationId, conversationId).orderByAsc(QaMessage::getCreateTime);
        List<QaMessage> messages = messageMapper.selectList(wrapper);
        return ResponseResult.okResult(Map.of("conversation", conv, "messages", messages));
    }

    @Override
    public ResponseResult feedback(Long userId, Long messageId, int score) {
        QaMessage msg = messageMapper.selectById(messageId);
        if (msg == null) {
            return ResponseResult.errorResult(400, "消息不存在");
        }
        msg.setQualityScore(score);
        messageMapper.updateById(msg);
        log.info("User {} feedback on message {}: score={}", userId, messageId, score);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private QaMessage saveMessage(Long userId, Long conversationId, String question, String answer, String sourcesJson,
                                  ChatResult chatResult, String searchType) {
        QaMessage msg = new QaMessage();
        msg.setConversationId(conversationId);
        msg.setUserId(userId);
        msg.setQuestion(question);
        msg.setAnswer(answer);
        msg.setSourceChunks(sourcesJson);
        msg.setCreateTime(LocalDateTime.now());
        msg.setUpdateTime(LocalDateTime.now());
        msg.setIsDeleted(DeleteConstants.NOT_DELETED);
        msg.setTokensUsed(chatResult != null ? chatResult.getTokenUsage() : 0);
        msg.setLlmModel(chatResult != null ? chatResult.getModel() : null);
        msg.setResponseMs(chatResult != null ? chatResult.getResponseTimeMs() : 0L);
        msg.setSearchType(searchType);
        messageMapper.insert(msg);
        return msg;
    }
}
