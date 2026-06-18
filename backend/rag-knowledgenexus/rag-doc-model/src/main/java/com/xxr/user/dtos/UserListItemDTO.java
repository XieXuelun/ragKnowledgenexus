package com.xxr.user.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserListItemDTO {
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String email;
    
    private String avatarUrl;
    
    private Integer role;
    
    private Long deptId;
    
    private Integer status;
    
    private LocalDateTime lastLoginTime;
}
