package com.xxr.constant;

public class WorkOrderConstants {

    /** 待处理 */
    public static final int STATUS_PENDING = 0;
    /** 处理中 */
    public static final int STATUS_PROCESSING = 1;
    /** 已解决 */
    public static final int STATUS_RESOLVED = 2;
    /** 关闭 */
    public static final int STATUS_CLOSED = 3;

    /** 优先级：低 */
    public static final int PRIORITY_LOW = 1;
    /** 优先级：中 */
    public static final int PRIORITY_MEDIUM = 2;
    /** 优先级：高 */
    public static final int PRIORITY_HIGH = 3;

    public static String getStatusDesc(int status) {
        switch (status) {
            case STATUS_PENDING: return "待处理";
            case STATUS_PROCESSING: return "处理中";
            case STATUS_RESOLVED: return "已解决";
            case STATUS_CLOSED: return "关闭";
            default: return "未知状态";
        }
    }
    public static String getPriorityDesc(int priority) {
        switch (priority) {
            case PRIORITY_LOW: return "低";
            case PRIORITY_MEDIUM: return "中";
            case PRIORITY_HIGH: return "高";
            default: return "未知";
        }
    }
}