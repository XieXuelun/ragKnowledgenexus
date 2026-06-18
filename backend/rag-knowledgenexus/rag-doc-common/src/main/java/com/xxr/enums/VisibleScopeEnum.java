package com.xxr.enums;

import lombok.Getter;

/**
 * 知识库可见范围枚举
 * 0 = 私有（仅自己）
 * 1 = 部门（本部门可见）
 * 2 = 全员（全公司可见）
 */
@Getter
public enum VisibleScopeEnum {

    PRIVATE(0, "私有"),
    DEPT(1, "部门"),
    PUBLIC(2, "全员");

    private final Integer code;
    private final String desc;

    VisibleScopeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}