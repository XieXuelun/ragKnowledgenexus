package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxr.user.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper extends BaseMapper<User> {
}
