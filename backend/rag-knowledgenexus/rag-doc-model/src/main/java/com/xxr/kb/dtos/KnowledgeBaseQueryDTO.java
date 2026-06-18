package com.xxr.kb.dtos;

import lombok.Data;

@Data
public class KnowledgeBaseQueryDTO {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Integer deptId;
    private String keyword;       // 名称/描述搜索
    private Integer visibility;   // 按可见范围筛选
    private String sortBy;        // 排序字段: createTime, docCount, viewCount
    private String sortOrder;     // asc/desc
}