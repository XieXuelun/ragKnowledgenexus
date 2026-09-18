package com.xxr.service;

import com.xxr.auth.dtos.UserRegisterDTO;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.UserRoleConstant;
import com.xxr.mapper.AuthMapper;
import com.xxr.service.impl.UserLoginServiceImpl;
import com.xxr.support.TestAuthentication;
import com.xxr.user.dtos.UserUpdateRequest;
import com.xxr.user.pojo.User;
import com.xxr.utils.MinioUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginServiceImplTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private MinioUtil minioUtil;

    @InjectMocks
    private UserLoginServiceImpl userLoginService;

    @AfterEach
    void clearSecurityContext() {
        TestAuthentication.clear();
    }

    @Test
    void registerForcesEmployeeRole() {
        UserRegisterDTO request = new UserRegisterDTO();
        request.setUsername("new-user");
        request.setPassword("password123");
        request.setRole(UserRoleConstant.KB_ADMIN);
        when(authMapper.selectCount(any())).thenReturn(0L);
        when(authMapper.insert(any(User.class))).thenReturn(1);

        ResponseResult result = userLoginService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insert(captor.capture());
        assertEquals(AppHttpCodeEnum.SUCCESS.getCode(), result.getCode());
        assertEquals(UserRoleConstant.EMPLOYEE, captor.getValue().getRole());
    }

    @Test
    void updateUserAllowsCurrentUserAndOnlyUpdatesWhitelistedFields() {
        TestAuthentication.authenticate(7L, UserRoleConstant.EMPLOYEE);
        User currentUser = activeUser(7L, UserRoleConstant.EMPLOYEE);
        when(authMapper.selectById(7L)).thenReturn(currentUser);
        when(authMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(7L);
        request.setNickname("new-name");
        request.setEmail("new@example.com");
        request.setDeptId(99L);

        ResponseResult result = userLoginService.updateUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).updateById(captor.capture());
        assertEquals(AppHttpCodeEnum.SUCCESS.getCode(), result.getCode());
        assertEquals("new-name", captor.getValue().getNickname());
        assertEquals("new@example.com", captor.getValue().getEmail());
        assertEquals(null, captor.getValue().getDeptId());
        assertEquals(null, captor.getValue().getRole());
    }

    @Test
    void updateUserRejectsOtherUser() {
        TestAuthentication.authenticate(7L, UserRoleConstant.EMPLOYEE);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(8L);
        request.setNickname("not-allowed");

        ResponseResult result = userLoginService.updateUser(request);

        assertEquals(AppHttpCodeEnum.NO_OPERATOR_AUTH.getCode(), result.getCode());
        verify(authMapper, never()).updateById(any(User.class));
    }

    private static User activeUser(Long id, int role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(1);
        user.setIsDeleted(0);
        return user;
    }
}
