package com.xxr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxr.auth.dtos.LoginDto;
import com.xxr.auth.dtos.UserRegisterDTO;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.user.dtos.UserUpdateRequest;
import com.xxr.user.pojo.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserLoginService extends IService<User> {
    /**
     * 用户登录
     * @param loginDto
     * @return
     */
    ResponseResult login(LoginDto loginDto);

    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    ResponseResult register(UserRegisterDTO userRegisterDTO);

    /**
     * 刷新Token
     * @param token 原token
     * @return
     */
    ResponseResult refreshToken(String token);

    /**
     * 用户登出
     * @return
     */
    ResponseResult logout();

    /**
     * 获取用户信息
     * @return
     */
    ResponseResult<User> getUserInfo();

    /**
     * 修改用户信息
     * @param userUpdateRequest
     * @return
     */
    ResponseResult updateUser( UserUpdateRequest userUpdateRequest);

    /**
     * 上传用户头像
     * @param file
     * @return
     */
    ResponseResult uploadAvatar(MultipartFile file) throws Exception;
}
