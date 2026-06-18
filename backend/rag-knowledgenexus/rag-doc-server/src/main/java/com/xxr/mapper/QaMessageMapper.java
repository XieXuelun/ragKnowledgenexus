package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxr.qa.pojo.QaMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaMessageMapper extends BaseMapper<QaMessage> {
}