package com.xxr.enums;

public enum EmbedStatusEnum {

    PENDING(0, "待向量化"),
    SUCCESS(1, "完成"),
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    EmbedStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    // 根据code获取枚举
    public static EmbedStatusEnum fromCode(int code) {
        for (EmbedStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知向量化状态码: " + code);
    }
}