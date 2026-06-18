package com.xxr.qa.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class QaFeedbackDTO {
    @NotNull
    private Long messageId;
    @NotNull
    private Integer score;
    private String reason;
}