package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.UserRoleConstant;
import com.xxr.kb.pojo.DocKnowledgeBase;
import com.xxr.mapper.KnowledgeMapper;
import com.xxr.mapper.UserMapper;
import com.xxr.security.SecurityUtils;
import com.xxr.user.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserMapper userMapper;
    private final KnowledgeMapper knowledgeMapper;

    public boolean isManager() {
        return isManager(SecurityUtils.getCurrentUserId());
    }

    public boolean isManager(Long userId) {
        User user = findActiveUser(userId);
        return user != null && hasManagerRole(user);
    }

    public boolean isSuperAdmin() {
        User user = findActiveUser(SecurityUtils.getCurrentUserId());
        return user != null && Integer.valueOf(UserRoleConstant.SUPER_ADMIN).equals(user.getRole());
    }

    public boolean isKbAdmin() {
        User user = findActiveUser(SecurityUtils.getCurrentUserId());
        return user != null && Integer.valueOf(UserRoleConstant.KB_ADMIN).equals(user.getRole());
    }

    public ResponseResult checkAdminPermission() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH, "用户未登录");
        }
        User user = findActiveUser(userId);
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
        }
        if (!hasManagerRole(user)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH, "用户不是管理员");
        }
        return null;
    }

    public ResponseResult checkSuperAdminPermission() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH, "用户未登录");
        }
        User user = findActiveUser(userId);
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
        }
        if (!Integer.valueOf(UserRoleConstant.SUPER_ADMIN).equals(user.getRole())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH, "仅超级管理员可操作");
        }
        return null;
    }

    public boolean canManageKnowledgeBase(Long kbId) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findActiveUser(userId);
        if (user == null || kbId == null) {
            return false;
        }
        if (Integer.valueOf(UserRoleConstant.SUPER_ADMIN).equals(user.getRole())) {
            return true;
        }
        if (!Integer.valueOf(UserRoleConstant.KB_ADMIN).equals(user.getRole())) {
            return false;
        }
        DocKnowledgeBase knowledgeBase = findActiveKnowledgeBase(kbId);
        return knowledgeBase != null && userId.equals(knowledgeBase.getOwnerId());
    }

    public boolean canViewKnowledgeBase(Long kbId) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findActiveUser(userId);
        if (user == null || kbId == null) {
            return false;
        }
        if (Integer.valueOf(UserRoleConstant.SUPER_ADMIN).equals(user.getRole())) {
            return true;
        }
        DocKnowledgeBase knowledgeBase = findActiveKnowledgeBase(kbId);
        if (knowledgeBase == null) {
            return false;
        }
        if (userId.equals(knowledgeBase.getOwnerId())) {
            return true;
        }
        if (Integer.valueOf(2).equals(knowledgeBase.getVisibility())) {
            return true;
        }
        return Integer.valueOf(1).equals(knowledgeBase.getVisibility())
                && user.getDeptId() != null
                && user.getDeptId().equals(knowledgeBase.getDeptId());
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null
                || Integer.valueOf(0).equals(user.getStatus())
                || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return null;
        }
        return user;
    }

    private DocKnowledgeBase findActiveKnowledgeBase(Long kbId) {
        DocKnowledgeBase knowledgeBase = knowledgeMapper.selectById(kbId);
        if (knowledgeBase == null || Integer.valueOf(1).equals(knowledgeBase.getIsDeleted())) {
            return null;
        }
        return knowledgeBase;
    }

    private boolean hasManagerRole(User user) {
        Integer role = user.getRole();
        return Integer.valueOf(UserRoleConstant.SUPER_ADMIN).equals(role)
                || Integer.valueOf(UserRoleConstant.KB_ADMIN).equals(role);
    }
}
