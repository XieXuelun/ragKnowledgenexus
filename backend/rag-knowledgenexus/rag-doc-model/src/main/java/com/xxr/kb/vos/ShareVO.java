package com.xxr.kb.vos;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ShareVO {
    private String shareUrl;
    private String shareCode;
    private Instant expireTime;
    private Boolean hasPassword;
}