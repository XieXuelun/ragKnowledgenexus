package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxr.common.dtos.PageResponseResult;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.constant.DeleteConstants;
import com.xxr.constant.WorkOrderConstants;
import com.xxr.mapper.TicketOrderMapper;
import com.xxr.mapper.UserMapper;
import com.xxr.service.PermissionService;
import com.xxr.service.TicketOrderService;
import com.xxr.ticket.dto.TicketCreateDTO;
import com.xxr.ticket.dto.TicketResolveDTO;
import com.xxr.ticket.pojo.TicketOrder;
import com.xxr.user.pojo.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketOrderServiceImpl implements TicketOrderService {

    private final TicketOrderMapper ticketMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;

    @Override
    public ResponseResult create(Long userId, TicketCreateDTO dto) {
        TicketOrder order = new TicketOrder();
        order.setUserId(userId);
        order.setTitle(dto.getTitle());
        order.setDescription(dto.getDescription());
        order.setPriority(dto.getPriority() != null ? dto.getPriority() : WorkOrderConstants.PRIORITY_MEDIUM);
        order.setStatus(WorkOrderConstants.STATUS_PENDING);
        order.setMessageId(dto.getMessageId());
        order.setKbId(dto.getKbId());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setIsDeleted(DeleteConstants.NOT_DELETED);
        ticketMapper.insert(order);
        log.info("Ticket created: id={}, userId={}, title={}", order.getId(), userId, dto.getTitle());
        return ResponseResult.okResult
                (Map.of("id", order.getId(), "title", order.getTitle(), "status", order.getStatus()));
    }

    @Override
    public ResponseResult list(Long userId, int page, int pageSize, Integer status) {
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<>();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseResult.errorResult(400, "用户不存在");
        }
        //普通员工只能查询自己的工单，管理员可以查询全部
        if (!permissionService.isManager(userId)) {
            wrapper.eq(TicketOrder::getUserId, userId);
        }
        //管理员
        wrapper.eq(TicketOrder::getIsDeleted, DeleteConstants.NOT_DELETED);
        if (status != null) {
            wrapper.eq(TicketOrder::getStatus, status);
        }
        wrapper.orderByDesc(TicketOrder::getCreateTime);

        IPage<TicketOrder> result = ticketMapper.selectPage(new Page<>(page, pageSize), wrapper);
        // Map entities to VO with description fields
        List<Map<String, Object>> records = result.getRecords().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("createName",userMapper.selectById(t.getUserId()).getUsername());
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("description", t.getDescription());
            m.put("status", t.getStatus());
            m.put("statusDesc", WorkOrderConstants.getStatusDesc(t.getStatus()));
            m.put("priority", t.getPriority());
            m.put("priorityDesc", WorkOrderConstants.getPriorityDesc(t.getPriority()));
            m.put("messageId", t.getMessageId());
            m.put("kbId", t.getKbId());
            m.put("reply", t.getReply());
            m.put("createTime", t.getCreateTime());
            m.put("updateTime", t.getUpdateTime());
            m.put("resolvedTime", t.getResolvedTime());
            m.put("replyTime", t.getReplyTime());
            return m;
        }).collect(Collectors.toList());

        return new PageResponseResult(page, pageSize, (int) result.getTotal()) {{
            setData(records);
        }};
    }

    @Override
    public ResponseResult resolve(Long userId, Long ticketId, TicketResolveDTO dto) {
        TicketOrder order = ticketMapper.selectById(ticketId);
        if (order == null) {
            return ResponseResult.errorResult(400, "工单不存在");
        }
        if (order.getStatus() == WorkOrderConstants.STATUS_RESOLVED ||
            order.getStatus() == WorkOrderConstants.STATUS_CLOSED) {
            return ResponseResult.errorResult(400, "工单已结束，无法重复处理");
        }
        order.setStatus(WorkOrderConstants.STATUS_RESOLVED);
        order.setAssigneeId(userId);
        order.setReply(dto.getReply());
        order.setReplyTime(LocalDateTime.now());
        order.setResolvedTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        ticketMapper.updateById(order);
        log.info("Ticket resolved: id={}, byUserId={}", ticketId, userId);
        return ResponseResult.okResult
                (Map.of("id", order.getId(), "status", order.getStatus(), "reply", order.getReply()));
    }
}
