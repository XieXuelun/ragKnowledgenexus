package com.xxr.constant;

public class DocumentParseStatusConstants {

    // 私有构造，防止实例化
    private DocumentParseStatusConstants() {}

    /**
     * 待处理
     */
    public static final int PENDING = 0;

    /**
     * 解析中
     */
    public static final int PARSING = 1;

    /**
     * 解析完成
     */
    public static final int SUCCESS = 2;

    /**
     * 解析失败
     */
    public static final int FAILED = 3;
}