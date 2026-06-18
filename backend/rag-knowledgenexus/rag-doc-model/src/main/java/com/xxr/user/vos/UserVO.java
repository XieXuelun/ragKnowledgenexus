package com.xxr.user.vos;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Integer role;
    private Long deptId;
    private String deptName;
    private Integer status;           // 0=禁用, 1=正常
    private Instant lastLoginTime;
    private Instant createTime;
}