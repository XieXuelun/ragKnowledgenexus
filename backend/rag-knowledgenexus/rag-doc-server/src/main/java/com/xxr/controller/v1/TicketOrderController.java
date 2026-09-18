package com.xxr.controller.v1;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.qa.dto.QaFeedbackDTO;
import com.xxr.security.SecurityUtils;
import com.xxr.service.TicketOrderService;
import com.xxr.ticket.dto.TicketCreateDTO;
import com.xxr.ticket.dto.TicketResolveDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/ticket")
@Api(tags = "工单管理接口")
public class TicketOrderController {

    @Autowired
    private TicketOrderService ticketOrderService;
    @PostMapping("/create")
    @ApiOperation("创建工单")
    public ResponseResult create(@Valid @RequestBody TicketCreateDTO dto) {
        return ticketOrderService.create(SecurityUtils.getCurrentUserId(), dto);
    }

    @GetMapping("/list")
    @ApiOperation("获取工单列表")
    public ResponseResult list(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) Integer status) {
        return ticketOrderService.list(SecurityUtils.getCurrentUserId(), page, pageSize, status);
    }

    @PutMapping("/{id}/resolve")
    @ApiOperation("管理员解决工单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'KB_ADMIN')")
    public ResponseResult resolve(@PathVariable Long id, @Valid @RequestBody TicketResolveDTO dto) {
        return ticketOrderService.resolve(SecurityUtils.getCurrentUserId(), id, dto);
    }
}
