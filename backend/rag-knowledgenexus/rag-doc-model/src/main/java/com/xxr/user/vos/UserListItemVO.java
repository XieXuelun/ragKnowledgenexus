package com.xxr.user.vos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserListItemVO {
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String email;
    
    private String avatarUrl;
    
    private Integer role;
    
    private Long deptId;
    
    private String deptName;
    
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;
}
