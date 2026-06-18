package com.xxr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.ticket.dto.TicketCreateDTO;
import com.xxr.ticket.dto.TicketResolveDTO;

public interface TicketOrderService {
    ResponseResult create(Long userId, TicketCreateDTO dto);
    ResponseResult list(Long userId, int page, int pageSize, Integer status);
    ResponseResult resolve(Long userId, Long ticketId, TicketResolveDTO dto);
}