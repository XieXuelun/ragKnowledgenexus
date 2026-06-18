package com.xxr.kb.dtos;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class ShareRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    private Integer expireDays;   // 有效期天数，为空则永久

    private String password;      // 访问密码（可选）
}