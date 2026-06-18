package com.xxr.ticket.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TicketCreateDTO {
    @NotBlank
    private String title;
    private String description;
    private Integer priority = 2;
    private Long messageId;
    private Long kbId;
}