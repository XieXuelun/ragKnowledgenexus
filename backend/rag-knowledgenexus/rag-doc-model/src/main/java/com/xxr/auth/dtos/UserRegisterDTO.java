package com.xxr.auth.dtos;

import lombok.Data;

/**
 * 新用户注册请求DTO
 */
@Data
public class UserRegisterDTO {
    private Long id;

    /**
     * 用户名（唯一，4-50字符
     */
    private String username;

    /**
     * 密码（6-100字符）
     */
    private String password;

    /**
     * 显示名称
     */
    private String nickname;

    /**
     * 邮箱地址（非必需）
     */
    private String email;

    /**
     * 所属部门ID（非必需）
     */
    private Long deptId;
    private Integer role;
}
