package com.xxr.security;

import com.xxr.mapper.UserMapper;
import com.xxr.user.pojo.User;
import com.xxr.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenPopulatesSecurityContext() throws Exception {
        User user = activeUser(7L, 1);
        when(userMapper.selectById(7L)).thenReturn(user);

        MockHttpServletRequest request = requestWithToken(JwtUtil.getToken(7L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(userMapper);
        AtomicReference<Authentication> authenticationDuringRequest = new AtomicReference<>();
        doAnswer(invocation -> {
            authenticationDuringRequest.set(SecurityContextHolder.getContext().getAuthentication());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = authenticationDuringRequest.get();
        assertNotNull(authentication);
        assertEquals("ROLE_KB_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidTokenReturnsUnauthorizedAndStopsChain() throws Exception {
        MockHttpServletRequest request = requestWithToken("not-a-valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(userMapper);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void disabledUserReturnsUnauthorizedAndStopsChain() throws Exception {
        User user = activeUser(7L, 2);
        user.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(user);

        MockHttpServletRequest request = requestWithToken(JwtUtil.getToken(7L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(userMapper);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unknownRoleReturnsUnauthorizedAndStopsChain() throws Exception {
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L, 99));

        MockHttpServletRequest request = requestWithToken(JwtUtil.getToken(7L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(userMapper);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/info");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static User activeUser(Long id, int role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRole(role);
        user.setStatus(1);
        user.setIsDeleted(0);
        return user;
    }
}
