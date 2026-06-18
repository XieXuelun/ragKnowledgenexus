package com.xxr.kb.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xxr.common.pojo.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data

@TableName("rag_knowledge_base.doc_knowledge_base_favorite ")
public class KbFavorite {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private LocalDateTime createTime;
    private Integer isDeleted;
    private Long userId;
    @TableField("kbId")
    private Long kbId;
}
