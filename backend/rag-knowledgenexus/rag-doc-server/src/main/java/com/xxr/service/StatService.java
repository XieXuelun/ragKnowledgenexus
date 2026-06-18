package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;

public interface StatService {
    ResponseResult getDailyStats(String startDate, String endDate);
    ResponseResult getHotQuestions(Long kbId, Integer topN);
}
