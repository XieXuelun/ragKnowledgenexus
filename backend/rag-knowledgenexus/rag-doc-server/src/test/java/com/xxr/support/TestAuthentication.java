package com.xxr.support;

import com.xxr.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TestAuthentication {

    private TestAuthentication() {
    }

    public static void authenticate(Long userId, int role) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "test-user", role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
