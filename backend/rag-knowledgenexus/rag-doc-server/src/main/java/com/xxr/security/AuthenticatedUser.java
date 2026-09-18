package com.xxr.security;

import com.xxr.constant.UserRoleConstant;
import com.xxr.user.pojo.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final Integer role;
    private final boolean enabled;

    public AuthenticatedUser(Long id, String username, Integer role) {
        this(id, username, role, true);
    }

    public AuthenticatedUser(Long id, String username, Integer role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
    }

    public static AuthenticatedUser from(User user) {
        boolean enabled = user != null
                && Integer.valueOf(1).equals(user.getStatus())
                && !Integer.valueOf(1).equals(user.getIsDeleted());
        return new AuthenticatedUser(
                user == null ? null : user.getId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getRole(),
                enabled
        );
    }

    public static boolean hasSupportedRole(Integer role) {
        return Integer.valueOf(UserRoleConstant.SUPER_ADMIN).equals(role)
                || Integer.valueOf(UserRoleConstant.KB_ADMIN).equals(role)
                || Integer.valueOf(UserRoleConstant.EMPLOYEE).equals(role);
    }

    public Long getId() {
        return id;
    }

    public Integer getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return switch (role) {
            case UserRoleConstant.SUPER_ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            case UserRoleConstant.KB_ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_KB_ADMIN"));
            case UserRoleConstant.EMPLOYEE -> List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
            default -> List.of();
        };
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
