package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxr.dept.pojo.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
