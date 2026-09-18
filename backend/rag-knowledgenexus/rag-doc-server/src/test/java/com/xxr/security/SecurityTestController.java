package com.xxr.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class SecurityTestController {

    @GetMapping("/public/hello")
    public String publicEndpoint() {
        return "public";
    }

    @GetMapping("/secured")
    public String securedEndpoint() {
        return "secured";
    }

    @GetMapping("/super")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String superAdminEndpoint() {
        return "super";
    }
}
