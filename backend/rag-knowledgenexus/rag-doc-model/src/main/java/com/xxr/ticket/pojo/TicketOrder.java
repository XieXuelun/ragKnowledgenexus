package com.xxr.ticket.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket_order")
public class TicketOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long messageId;

    private Long userId;

    private String title;

    private String description;

    private Integer status;

    private Long assigneeId;

    private Integer priority;

    private Long kbId;

    @TableField("created_time")
    private LocalDateTime createTime;

    @TableField("updated_time")
    private LocalDateTime updateTime;

    private LocalDateTime resolvedTime;

    private String reply;

    private LocalDateTime replyTime;

    private Integer isDeleted;
}