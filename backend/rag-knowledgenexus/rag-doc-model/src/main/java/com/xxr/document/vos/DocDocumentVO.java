package com.xxr.document.vos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xxr.common.pojo.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_document")
public class DocDocumentVO extends BaseEntity {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long kbId;
    
    private String title;
    
    private String fileName;
    
    private String fileType;  // pdf / docx / txt
    
    private Long fileSize;
    
    private String minioPath;
    
    private Integer parseStatus;  // 0=待处理 1=解析中 2=完成 3=失败
    
    private String parseError;
    
    private Integer chunkCount;
    
    private Long uploadUserId;
    
    private Integer wordCount;
    private String knowledgeName;
    private Integer isDeleted;
}