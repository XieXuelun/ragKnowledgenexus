package com.xxr.user.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xxr.common.pojo.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String username;
    
    private String password;
    
    private String nickname;
    
    private String email;
    
    private String avatarUrl;
    
    private Integer role;  // 0=超管 1=管理员 2=普通员工
    
    private Long deptId;
    
    private Integer status;
    // 0=禁用 1=正常
    private Integer isDeleted;
    
    private LocalDateTime lastLoginTime;
}