package com.xxr.stat.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stat_hot_question")
public class StatHotQuestion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String questionHash;
    private String question;
    private Long kbId;
    private Integer askCount;
    private LocalDateTime lastAskTime;
    private java.math.BigDecimal avgQuality;
    private LocalDateTime updateTime;
}
