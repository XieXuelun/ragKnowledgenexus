package com.xxr.qa.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ConversationCreateDTO {

    @NotNull
    private Long kbId;

    private String title;
}