package com.xxr.controller.v1;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.service.StatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stat")
@Api(tags = "数据统计接口")
public class StatController {

    @Autowired
    private StatService statService;

    @GetMapping("/daily")
    @ApiOperation("获取每日统计概览")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseResult getDailyStats(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        return statService.getDailyStats(startDate, endDate);
    }

    @GetMapping("/hot-questions")
    @ApiOperation("获取热门问题 Top-N")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseResult getHotQuestions(@RequestParam(required = false) Long kbId,
                                          @RequestParam(defaultValue = "20") Integer topN) {
        return statService.getHotQuestions(kbId, topN);
    }
}
