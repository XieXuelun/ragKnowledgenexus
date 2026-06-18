package com.xxr.controller.v1;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.qa.dto.ConversationCreateDTO;
import com.xxr.qa.dto.QaAskDTO;
import com.xxr.qa.dto.QaFeedbackDTO;
import com.xxr.service.QaService;
import com.xxr.utils.BaseContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/qa")
@Api(tags = "智能问答接口")
public class QaController {

    @Autowired
    private QaService qaService;

    @PostMapping("/conversation/create")
    @ApiOperation("创建新对话")
    public ResponseResult createConversation(@Valid @RequestBody ConversationCreateDTO dto) {
        return qaService.createConversation(BaseContext.getCurrentId(), dto);
    }

    @PostMapping("/ask")
    @ApiOperation("智能问答")
    public ResponseResult ask(@Valid @RequestBody QaAskDTO dto) {
        return qaService.ask(BaseContext.getCurrentId(), dto);
    }

    @GetMapping("/conversation/list")
    @ApiOperation("获取对话列表")
    public ResponseResult getConversations(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String keyword) {
        return qaService.getConversations(BaseContext.getCurrentId(), page, pageSize, keyword);
    }

    @GetMapping("/conversation/detail/{conversationId}")
    @ApiOperation("获取对话详情")
    public ResponseResult getConversationDetail(@PathVariable Long conversationId) {
        return qaService.getConversationDetail(BaseContext.getCurrentId(), conversationId);
    }

    @PostMapping("/feedback")
    @ApiOperation("提交反馈")
    public ResponseResult feedback(@Valid @RequestBody QaFeedbackDTO dto) {
        return qaService.feedback(BaseContext.getCurrentId(), dto.getMessageId(), dto.getScore());
    }
}