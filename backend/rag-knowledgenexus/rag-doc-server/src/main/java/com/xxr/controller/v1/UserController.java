package com.xxr.controller.v1;

import com.xxr.auth.dtos.LoginDto;
import com.xxr.auth.dtos.UserRegisterDTO;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.service.UserLoginService;
import com.xxr.service.UserService;
import com.xxr.user.dtos.UserQueryDTO;
import com.xxr.user.dtos.UserUpdateRequest;
import com.xxr.user.pojo.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/user")
@Slf4j
@Api(tags = "用户管理接口")
public class UserController {
    
    @Autowired
    private UserLoginService userLoginService;
    
    @Autowired
    private UserService userService;

    /**
     * 用户登录，获取token
     * @param loginDto
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录，获取token")
    public ResponseResult login(@RequestBody LoginDto loginDto) {
        log.info("收到登录请求，用户名: {}", loginDto.getUsername());
        ResponseResult result = userLoginService.login(loginDto);
        if (result.getCode() == 200) {
            log.info("登录成功，用户名: {}", loginDto.getUsername());
        } else {
            log.warn("登录失败，用户名: {}，错误信息: {}", loginDto.getUsername(), result.getErrorMessage());
        }
        return result;
    }

    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册")
    public ResponseResult register(@RequestBody UserRegisterDTO userRegisterDTO) {
        return userLoginService.register(userRegisterDTO);
    }

    /**
     * 刷新Token
     * @param request
     * @return
     */
    @PostMapping("/refresh")
    @ApiOperation(value = "刷新Token")
    public ResponseResult<?> refreshToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        log.info("收到Token刷新请求");
        return userLoginService.refreshToken(token);
    }

    /**
     * 获取用户信息
     * @return
     */
    @GetMapping("/info")
    @ApiOperation(value = "获取用户信息")
    public ResponseResult getUserInfo() {
        return userLoginService.getUserInfo();
    }

    /**
     * 用户登出
     * @return
     */
    @ApiOperation(value = "用户登出")
    @PostMapping("/logout")
    public ResponseResult<?> logout() {
        log.info("收到登出请求");
        return userLoginService.logout();
    }

    /**
     * 更新用户信息
     * @return
     */
    @ApiOperation(value = "更新用户信息")
    @PostMapping("/update")
    public ResponseResult updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        return userLoginService.updateUser(userUpdateRequest);
    }

    /**
     * 上传用户头像
     * @param file
     * @return
     */
    @ApiOperation(value = "上传用户头像")
    @PostMapping("/avatar")
    public ResponseResult uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        return userLoginService.uploadAvatar(file);
    }

    // ========== 管理员用户管理接口 ==========

    /**
     * 获取用户列表（管理员）
     * @param userQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation(value = "获取用户列表")
    public ResponseResult page(UserQueryDTO userQueryDTO) {
        return userService.pageQuery(userQueryDTO);
    }

    /**
     * 删除用户（管理员）
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    @ApiOperation(value = "删除用户")
    public ResponseResult delete(@PathVariable Long id){
        return userService.deleteUser(id);
    }

    /**
     * 设置用户状态（管理员）
     * @return
     */
    @PostMapping("/status")
    @ApiOperation(value = "设置用户状态")
    public ResponseResult setStatus(@RequestBody User user){
        return userService.setStatus(user);
    }
}