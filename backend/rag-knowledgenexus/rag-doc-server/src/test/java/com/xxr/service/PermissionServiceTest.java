package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.constant.UserRoleConstant;
import com.xxr.mapper.UserMapper;
import com.xxr.user.pojo.User;
import com.xxr.utils.BaseContext;
import com.xxr.utils.CurrentUserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserMapper userMapper;

    private CurrentUserUtil currentUserUtil;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        currentUserUtil = new CurrentUserUtil();
        permissionService = new PermissionService(userMapper, currentUserUtil);
    }

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void isManagerReturnsFalseWhenNoUserIsLoggedIn() {
        assertFalse(permissionService.isManager());
    }

    @Test
    void isManagerReturnsTrueForSuperAdmin() {
        mockCurrentUser(UserRoleConstant.SUPER_ADMIN);

        assertTrue(permissionService.isManager());
    }

    @Test
    void isManagerReturnsTrueForKnowledgeBaseAdmin() {
        mockCurrentUser(UserRoleConstant.KB_ADMIN);

        assertTrue(permissionService.isManager());
    }

    @Test
    void isManagerReturnsFalseForEmployee() {
        mockCurrentUser(UserRoleConstant.EMPLOYEE);

        assertFalse(permissionService.isManager());
    }

    @Test
    void isManagerReturnsFalseForUnknownRole() {
        mockCurrentUser(99);

        assertFalse(permissionService.isManager());
    }

    @Test
    void isManagerReturnsFalseForDisabledUser() {
        User user = activeUser(UserRoleConstant.KB_ADMIN);
        user.setStatus(0);
        BaseContext.setCurrentId(user.getId());
        when(userMapper.selectById(user.getId())).thenReturn(user);

        assertFalse(permissionService.isManager());
    }

    @Test
    void checkAdminPermissionRequiresLogin() {
        ResponseResult result = permissionService.checkAdminPermission();

        assertEquals(AppHttpCodeEnum.NEED_LOGIN.getCode(), result.getCode());
    }

    @Test
    void checkAdminPermissionRejectsMissingUser() {
        BaseContext.setCurrentId(42L);
        when(userMapper.selectById(42L)).thenReturn(null);

        ResponseResult result = permissionService.checkAdminPermission();

        assertEquals(AppHttpCodeEnum.DATA_NOT_EXIST.getCode(), result.getCode());
    }

    @Test
    void checkAdminPermissionRejectsEmployee() {
        mockCurrentUser(UserRoleConstant.EMPLOYEE);

        ResponseResult result = permissionService.checkAdminPermission();

        assertEquals(AppHttpCodeEnum.NO_OPERATOR_AUTH.getCode(), result.getCode());
    }

    @Test
    void checkAdminPermissionAllowsKnowledgeBaseAdmin() {
        mockCurrentUser(UserRoleConstant.KB_ADMIN);

        assertNull(permissionService.checkAdminPermission());
    }

    private void mockCurrentUser(int role) {
        User user = activeUser(role);
        BaseContext.setCurrentId(user.getId());
        when(userMapper.selectById(user.getId())).thenReturn(user);
    }

    private User activeUser(int role) {
        User user = new User();
        user.setId(42L);
        user.setRole(role);
        user.setStatus(1);
        user.setIsDeleted(DeleteConstants.NOT_DELETED);
        return user;
    }
}
