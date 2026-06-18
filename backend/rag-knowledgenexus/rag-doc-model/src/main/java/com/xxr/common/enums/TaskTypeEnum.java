package com.xxr.common.enums;

import lombok.Getter;

@Getter
public enum TaskTypeEnum {

    NEWS_SCAN_TIME(1001, 1, "文章定时审核"),
    REMOTEERROR(1002, 2, "第三方接口调用失败，重试");

    private final int taskType;
    private final int priority;
    private final String desc;

    TaskTypeEnum(int taskType, int priority, String desc) {
        this.taskType = taskType;
        this.priority = priority;
        this.desc = desc;
    }
}