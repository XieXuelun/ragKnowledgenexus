package com.xxr.stat.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_daily")
public class StatDaily {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private LocalDate statDate;
    private Integer totalQa;
    private Integer totalUsers;
    private java.math.BigDecimal positiveRate;
    private Integer transferCount;
    private Integer avgResponseMs;
    private Integer newDocs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
