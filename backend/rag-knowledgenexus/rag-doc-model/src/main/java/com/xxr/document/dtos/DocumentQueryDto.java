package com.xxr.document.dtos;

import lombok.Data;

@Data
public class DocumentQueryDto {
    private Long kbId;
    /**
     * 文档标题/文件名关键词
     */
    private String keyword;

    /**
     * 文件类型：pdf/docx/txt
     */
    private String fileType;

    /**
     * 解析状态：0/1/2/3
     * 0-待解析 1-解析中 2-解析成功 3-解析失败
     */
    private Integer parseStatus;

    /**
     * 页码（从1开始）
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer pageSize;
}