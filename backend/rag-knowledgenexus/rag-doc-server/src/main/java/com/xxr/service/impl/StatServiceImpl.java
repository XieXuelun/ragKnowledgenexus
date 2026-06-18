package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.mapper.StatDailyMapper;
import com.xxr.mapper.StatHotQuestionMapper;
import com.xxr.service.StatService;
import com.xxr.stat.pojo.StatDaily;
import com.xxr.stat.pojo.StatHotQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final StatDailyMapper statDailyMapper;
    private final StatHotQuestionMapper statHotQuestionMapper;

    @Override
    public ResponseResult getDailyStats(String startDate, String endDate) {
        LambdaQueryWrapper<StatDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(StatDaily::getStatDate);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(StatDaily::getStatDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(StatDaily::getStatDate, LocalDate.parse(endDate));
        }
        List<StatDaily> list = statDailyMapper.selectList(wrapper);
        return ResponseResult.okResult(list);
    }

    @Override
    public ResponseResult getHotQuestions(Long kbId, Integer topN) {
        LambdaQueryWrapper<StatHotQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StatHotQuestion::getAskCount);
        if (kbId != null) {
            wrapper.eq(StatHotQuestion::getKbId, kbId);
        }
        wrapper.last("LIMIT " + (topN != null ? topN : 20));
        List<StatHotQuestion> list = statHotQuestionMapper.selectList(wrapper);
        return ResponseResult.okResult(list);
    }
}
