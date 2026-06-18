package com.xxr.stat.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;
    
    private String module;  // doc/qa/ticket 等
    
    private String action;  // upload/delete/ask 等
    
    private Long targetId;
    
    private String ip;
    
    private String userAgent;
    
    private Integer costMs;
    
    private Integer result;  // 0=失败 1=成功
    
    private String failReason;
    
    private LocalDateTime createTime;
}