package com.xxr.user.dtos;

import lombok.Data;


@Data
public class UserQueryDTO {
    private Integer page=1 ;
    private Integer pageSize=10 ;
    private Long deptId;
    private Integer role;      // 0=超管, 1=KB管理员, 2=普通员工
    private String keyword;    // 用户名/昵称/邮箱模糊搜索
    private Integer status;    // 0=禁用, 1=正常


}