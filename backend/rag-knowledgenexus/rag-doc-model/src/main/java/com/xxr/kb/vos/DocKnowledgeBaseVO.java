package com.xxr.kb.vos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xxr.common.pojo.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data

public class DocKnowledgeBaseVO {
    

    private Long id;
    
    private String name;
    
    private String description;
    
    private Long ownerId;
    
    private Integer visibility;  // 0=私有 1=部门 2=全员
    
    private Long deptId;
    
    private Integer docCount;
    private Integer isDeleted;

    
    private String embedModel;
}