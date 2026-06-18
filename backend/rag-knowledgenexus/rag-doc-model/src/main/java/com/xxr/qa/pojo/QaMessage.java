package com.xxr.qa.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xxr.common.pojo.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qa_message")
public class QaMessage extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long conversationId;

    private Long userId;

    private String question;

    private String answer;

    /** JSON: [{chunkId, content, score}] */
    private String sourceChunks;

    /** 用户反馈评分 1=好评 -1=差评 */
    private Integer qualityScore;

    /** token 消耗数 */
    private Integer tokensUsed;

    /** LLM 模型名称 */
    private String llmModel;

    /** 搜索方式 hybrid / vector / fulltext */
    private String searchType;

    /** LLM 响应耗时 (ms) */
    private Long responseMs;
}