package com.xxr.constant;

/**
 * 逻辑删除相关常量
 */
public class DeleteConstants {

    /**
     * 未删除（正常状态）
     */
    public static final Integer NOT_DELETED = 0;

    /**
     * 已删除（逻辑删除状态）
     */
    public static final Integer DELETED = 1;

    // 私有构造，防止实例化
    private DeleteConstants() {}
}