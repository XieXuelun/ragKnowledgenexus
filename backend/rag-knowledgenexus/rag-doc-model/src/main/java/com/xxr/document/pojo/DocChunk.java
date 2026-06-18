package com.xxr.document.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xxr.common.pojo.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_chunk")
public class DocChunk extends BaseEntity {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long docId;
    
    private Long kbId;
    
    private Integer chunkIndex;
    
    private String content;
    
    private Integer contentLength;
    
    private String vectorId;
    
    private Integer embedStatus;  // 0=待向量化 1=完成 2=失败
    
    private Integer pageNum;
}