package com.xxr.interceptor;

import com.xxr.utils.CurrentUserUtil;
import com.xxr.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT令牌校验的拦截器 - 适用于RAG知识库项目
 */
@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private CurrentUserUtil currentUserUtil;

/**
     * 校验JWT令牌
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行
            return true;
        }

        // 从请求头中获取令牌
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 移除 "Bearer " 前缀
        }

        // 如果令牌为空，直接放行，让Controller去处理
        if (token == null || token.isEmpty()) {
            log.debug("令牌为空，放行请求");
            return true;
        }

        // 校验令牌
        try {
            log.debug("开始JWT校验: {}", token);
            Claims claims = JwtUtil.getClaimsBody(token);
            if (claims != null && JwtUtil.verifyToken(claims) == -1) { // -1 表示有效且未到刷新时间
                Long userId = Long.valueOf(claims.get("id").toString());
                log.debug("JWT校验成功，当前用户id: {}", userId);
                currentUserUtil.setCurrentId(userId);
                // 通过，放行
                return true;
            } else {
                log.warn("JWT令牌校验失败，令牌无效或已过期，用户请求被拒绝");
                response.setStatus(401);
                return false;
            }
        } catch (Exception ex) {
            log.error("JWT校验过程中发生异常: {}", ex.getMessage());
            // 不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        currentUserUtil.clearCurrentId();
    }
}
