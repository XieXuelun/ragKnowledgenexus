package com.xxr.qa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QaAskDTO {

    @NotNull
    private Long conversationId;

    @NotBlank
    private String question;

    private String searchType;

    private Integer topK = 5;
}
