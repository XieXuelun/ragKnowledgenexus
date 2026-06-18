package com.xxr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.user.dtos.UserQueryDTO;
import com.xxr.user.pojo.User;
import org.springframework.web.bind.annotation.RequestMapping;

public interface UserService extends IService<User> {
    /**
     * 获取用户列表
     * @param userQueryDTO
     * @return
     */
    ResponseResult pageQuery(UserQueryDTO userQueryDTO);

    /**
     * 删除用户
     * @param id
     * @return
     */
    ResponseResult deleteUser(Long id);

    /**
     * 设置用户状态
     * @param id
     * @param status
     * @return
     */
    ResponseResult setStatus(User user);
}
