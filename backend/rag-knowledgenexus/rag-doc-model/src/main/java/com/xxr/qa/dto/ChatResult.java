package com.xxr.qa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResult {
    private String content;
    private int tokenUsage;
    private String model;
    private long responseTimeMs;
}