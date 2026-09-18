package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.UserRoleConstant;
import com.xxr.mapper.UserMapper;
import com.xxr.user.pojo.User;
import com.xxr.utils.CurrentUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserMapper userMapper;
    private final CurrentUserUtil currentUserUtil;

    public boolean isManager() {
        return isManager(currentUserUtil.getCurrentId());
    }

    public boolean isManager(Long userId) {
        //判断用户是否存在且状态为正常
        User user = findActiveUser(userId);
        //不为空且有管理员角色
        return user!=null&&!hasManagerRole(user);
    }
    //检查用户是否有管理员权限
    public ResponseResult checkAdminPermission() {
        //获取用户id
        Long userId = currentUserUtil.getCurrentId();
        //判断是否登录
        if (userId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户未登录");
        }
        //判断用户是否存在且状态为正常
        User user = findActiveUser(userId);
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户不存在");
        }
        //判断用户是否有管理员角色
        if(!hasManagerRole(user)){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户不是管理员");
        }
        return null;
       }
    //查找用户是否存在且状态为正常
    private User findActiveUser(Long userId) {
        //判断用户id是否为空
        if (userId == null) {
            return null;
        }
        //根据用户id查询用户信息
        User user = userMapper.selectById(userId);
        //判断用户是否存在且状态为正常
        if(user==null||Integer.valueOf(0).equals(user.getStatus())){
            return null;
        }
        return user;
    }
    //判断用户是否有管理员角色
    private boolean hasManagerRole(User user) {
        Integer role = user.getRole();
        return role != null && (role == UserRoleConstant.SUPER_ADMIN || role == UserRoleConstant.KB_ADMIN);
    }
}
