package com.xxr.constant;

/**
 * 用户角色常量定义
 * 根据数据库设计，角色值定义如下：
 * 0 = 超管（超级管理员）
 * 1 = KB管理员
 * 2 = 普通员工
 */
public class UserRoleConstant {
    /**
     * 超级管理员角色值
     */
    public static final int SUPER_ADMIN = 0;
    
    /**
     * KB管理员角色值
     */
    public static final int KB_ADMIN = 1;
    
    /**
     * 普通员工角色值
     */
    public static final int EMPLOYEE = 2;
    
    /**
     * 获取角色描述
     * @param role 角色值
     * @return 角色描述
     */
    public static String getRoleDescription(int role) {
        switch (role) {
            case SUPER_ADMIN:
                return "超级管理员";
            case KB_ADMIN:
                return "KB管理员";
            case EMPLOYEE:
                return "普通员工";
            default:
                return "未知角色";
        }
    }
}