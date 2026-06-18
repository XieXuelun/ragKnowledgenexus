package com.xxr.auth.dtos;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@Data
public class LoginDto {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度为4-50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;  // MD5加密后的密码

    private Boolean rememberMe = false;
}