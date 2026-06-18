package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxr.auth.dtos.LoginDto;
import com.xxr.auth.dtos.UserRegisterDTO;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.constant.UserRoleConstant;
import com.xxr.mapper.AuthMapper;
import com.xxr.service.UserLoginService;
import com.xxr.user.dtos.UserUpdateRequest;
import com.xxr.user.pojo.User;
import com.xxr.utils.BaseContext;
import com.xxr.utils.JwtUtil;
import com.xxr.utils.MinioUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserLoginServiceImpl extends ServiceImpl<AuthMapper, User> implements UserLoginService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final int USERNAME_MIN_LENGTH = 4;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 100;

    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private MinioUtil minioUtil;

    /**
     * 用户登录
     *
     * @param loginDto
     * @return
     */
    @Override
    public ResponseResult login(LoginDto loginDto) {
        log.info("开始处理用户登录请求: {}", loginDto.getUsername());

        if (loginDto == null) {
            log.warn("登录失败: 参数为空");
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        String username = loginDto.getUsername();
        String password = loginDto.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("登录失败: 用户名或密码为空");
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户名或密码不能为空");
        }

        User user = authMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));

        if (user == null) {
            log.warn("登录失败: 用户不存在 - {}", username);
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户不存在");
        }

        if (user.getStatus() == 0) {
            log.warn("登录失败: 用户已被禁用 - {}", username);
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH, "用户已被禁用");
        }

        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            log.warn("登录失败: 密码错误 - {}", username);
            return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "密码错误");
        }

        log.info("登录成功: {}", username);
        String token = JwtUtil.getToken(user.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        user.setPassword(null);
        map.put("user", user);
        return ResponseResult.okResult(map);
    }

    /**
     * 用户注册
     *
     * @param userRegisterDTO
     * @return
     */
    @Override
    public ResponseResult register(UserRegisterDTO userRegisterDTO) {
        log.info("开始处理用户注册请求");

        if (userRegisterDTO == null) {
            log.warn("注册失败: 参数为空");
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "无效参数");
        }
        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();
       /* Long currentId = BaseContext.getCurrentId();*/
        int count = Math.toIntExact(authMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)));
        if (count > 0) {
            log.warn("注册失败: 用户名已存在 - {}", username);
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(userRegisterDTO, user);
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(1);
        user.setPassword(PASSWORD_ENCODER.encode(password));
        user.setIsDeleted(DeleteConstants.NOT_DELETED);
        user.setRole(userRegisterDTO.getRole());
        save(user);
        log.info("用户注册成功: {}", username);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 刷新Token
     *
     * @param token 原token
     * @return
     */
    @Override
    public ResponseResult refreshToken(String token) {
        if (token == null || token.isEmpty()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.TOKEN_REQUIRE);
        }
        try {
            Claims claims = JwtUtil.getClaimsBody(token);
            if (claims == null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.TOKEN_INVALID);
            }
            int verifyResult = JwtUtil.verifyToken(claims);
            if (verifyResult == 2) {
                return ResponseResult.errorResult(AppHttpCodeEnum.TOKEN_INVALID);
            }
            Long userId = Long.valueOf(claims.get("id").toString());
            User user = authMapper.selectById(userId);
            if (user == null || user.getStatus() == 0) {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在或已被禁用");
            }
            String newToken = JwtUtil.getToken(userId);
            Map<String, Object> map = new HashMap<>();
            map.put("token", newToken);
            return ResponseResult.okResult(map);
        } catch (Exception e) {
            return ResponseResult.errorResult(AppHttpCodeEnum.TOKEN_INVALID);
        }
    }

    /**
     * 用户登出
     *
     * @return
     */
    @Override
    public ResponseResult logout() {
        BaseContext.removeCurrentId();
        return ResponseResult.okResult("登出成功");
    }
    /**
     * 获取用户信息
     * @return
     */
    @Override
    public ResponseResult<User> getUserInfo() {
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();
        //校验参数
        if (currentId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, "请先登录");
        }
        //查询当前用户信息
        User user = authMapper.selectById(currentId);
        if (user == null || user.getStatus() == 0 || user.getIsDeleted() == DeleteConstants.DELETED) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在或已被禁用");
        }
        user.setPassword(null);
        // Convert stored avatar URL to presigned URL for browser access
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                String presignedUrl = minioUtil.getPresignedAvatarUrl(user.getAvatarUrl(), 86400);
                user.setAvatarUrl(presignedUrl);
            } catch (Exception ignored) {
                // Fallback: keep original URL
            }
        }
        //返回数据
        return ResponseResult.okResult(user);
    }

    /**
     * 修改用户信息
     *
     * @param userUpdateRequest
     * @return
     */
    @Override
    public ResponseResult updateUser(UserUpdateRequest userUpdateRequest) {
        if(userUpdateRequest == null||userUpdateRequest.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "无效参数");
        }
        User user =new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        //补全属性
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 上传用户头像
     *
     * @param file
     * @return
     */
    @Override
    public ResponseResult uploadAvatar(MultipartFile file) throws Exception {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "上传的文件不能为空");
        }
        String avatar = minioUtil.uploadAvatar(file);
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, "请先登录");
        }
        //更新用户信息
        User user=new User();
        user.setId(currentId);
        user.setAvatarUrl(avatar);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        // Return presigned URL for browser access
        String presignedUrl = minioUtil.getPresignedAvatarUrl(avatar, 86400);
        return ResponseResult.okResult(presignedUrl);
    }
}