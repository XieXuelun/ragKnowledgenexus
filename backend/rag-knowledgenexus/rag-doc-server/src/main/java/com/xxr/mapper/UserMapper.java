package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxr.user.pojo.User;
import com.xxr.user.vos.UserListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 分页查询用户列表（关联部门表）
     * @param page 分页对象
     * @param status 状态
     * @param role 角色
     * @param deptId 部门ID
     * @param keyword 关键字
     * @return 用户列表（包含部门名称）
     */
    IPage<UserListItemVO> selectUserPageWithDept(
            Page<UserListItemVO> page,
            @Param("status") Integer status,
            @Param("role") Integer role,
            @Param("deptId") Long deptId,
            @Param("keyword") String keyword
    );
}
