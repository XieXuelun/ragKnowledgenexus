package com.xxr.user.dtos;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Data
public class UserUpdateRequest {
    private Long id;
    @Size(max = 50, message = "昵称最大50个字符")
    private String userName;
    private String nickname;
    private String oldPassword;
    private String password;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最大100个字符")
    private String email;

    private Long deptId;
}
