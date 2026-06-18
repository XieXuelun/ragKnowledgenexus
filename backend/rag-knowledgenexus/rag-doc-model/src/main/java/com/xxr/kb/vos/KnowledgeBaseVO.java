package com.xxr.kb.vos;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class KnowledgeBaseVO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer visibility;
    private Long creatorId;
    private String creatorName;
    private Integer docCount;     // 文档数量
    private Integer qaCount;      // 问答数量
    private Integer viewCount;    // 浏览次数
    private Integer favoriteCount;// 收藏次数
    private Boolean isFavorite;   // 当前用户是否收藏
    private Instant createTime;
    private Instant updateTime;
}