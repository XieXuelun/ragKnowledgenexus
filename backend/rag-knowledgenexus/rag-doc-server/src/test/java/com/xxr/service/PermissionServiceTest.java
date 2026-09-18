package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.constant.UserRoleConstant;
import com.xxr.kb.pojo.DocKnowledgeBase;
import com.xxr.mapper.KnowledgeMapper;
import com.xxr.mapper.UserMapper;
import com.xxr.support.TestAuthentication;
import com.xxr.user.pojo.User;
import org.junit.jupiter.api.AfterEach;
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

    @Mock
    private KnowledgeMapper knowledgeMapper;

    @AfterEach
    void clearContext() {
        TestAuthentication.clear();
    }

    @Test
    void isManagerReturnsFalseWhenNoUserIsLoggedIn() {
        PermissionService permissionService = permissionService();
        assertFalse(permissionService.isManager());
    }

    @Test
    void isManagerReturnsTrueForSuperAdmin() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.SUPER_ADMIN);

        assertTrue(permissionService.isManager());
    }

    @Test
    void isManagerReturnsTrueForKnowledgeBaseAdmin() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.KB_ADMIN);

        assertTrue(permissionService.isManager());
    }

    @Test
    void isManagerReturnsFalseForEmployee() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.EMPLOYEE);

        assertFalse(permissionService.isManager());
    }

    @Test
    void isManagerReturnsFalseForUnknownRole() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(99);

        assertFalse(permissionService.isManager());
    }

    @Test
    void isManagerReturnsFalseForDisabledUser() {
        PermissionService permissionService = permissionService();
        User user = activeUser(UserRoleConstant.KB_ADMIN);
        user.setStatus(0);
        TestAuthentication.authenticate(user.getId(), user.getRole());
        when(userMapper.selectById(user.getId())).thenReturn(user);

        assertFalse(permissionService.isManager());
    }

    @Test
    void checkAdminPermissionRequiresLogin() {
        PermissionService permissionService = permissionService();
        ResponseResult result = permissionService.checkAdminPermission();

        assertEquals(AppHttpCodeEnum.NO_OPERATOR_AUTH.getCode(), result.getCode());
    }

    @Test
    void checkAdminPermissionRejectsMissingUser() {
        PermissionService permissionService = permissionService();
        TestAuthentication.authenticate(42L, UserRoleConstant.KB_ADMIN);
        when(userMapper.selectById(42L)).thenReturn(null);

        ResponseResult result = permissionService.checkAdminPermission();

        assertEquals(AppHttpCodeEnum.DATA_NOT_EXIST.getCode(), result.getCode());
    }

    @Test
    void checkAdminPermissionRejectsEmployee() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.EMPLOYEE);

        ResponseResult result = permissionService.checkAdminPermission();

        assertEquals(AppHttpCodeEnum.NO_OPERATOR_AUTH.getCode(), result.getCode());
    }

    @Test
    void checkAdminPermissionAllowsKnowledgeBaseAdmin() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.KB_ADMIN);

        assertNull(permissionService.checkAdminPermission());
    }

    @Test
    void canManageKnowledgeBaseAllowsKnowledgeBaseAdminForOwnedKnowledgeBase() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.KB_ADMIN);
        when(knowledgeMapper.selectById(10L)).thenReturn(knowledgeBase(10L, 42L));

        assertTrue(permissionService.canManageKnowledgeBase(10L));
    }

    @Test
    void canManageKnowledgeBaseRejectsKnowledgeBaseAdminForOtherKnowledgeBase() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.KB_ADMIN);
        when(knowledgeMapper.selectById(10L)).thenReturn(knowledgeBase(10L, 99L));

        assertFalse(permissionService.canManageKnowledgeBase(10L));
    }

    @Test
    void canManageKnowledgeBaseAllowsSuperAdminForAnyKnowledgeBase() {
        PermissionService permissionService = permissionService();
        mockCurrentUser(UserRoleConstant.SUPER_ADMIN);

        assertTrue(permissionService.canManageKnowledgeBase(10L));
    }

    private PermissionService permissionService() {
        return new PermissionService(userMapper, knowledgeMapper);
    }

    private void mockCurrentUser(int role) {
        User user = activeUser(role);
        TestAuthentication.authenticate(user.getId(), role);
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

    private static DocKnowledgeBase knowledgeBase(Long id, Long ownerId) {
        DocKnowledgeBase knowledgeBase = new DocKnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setIsDeleted(DeleteConstants.NOT_DELETED);
        return knowledgeBase;
    }
}
