package com.xxr.security;

import com.xxr.config.SecurityConfig;
import com.xxr.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @Test
    void publicEndpointAllowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/v1/test/public/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void securedEndpointRejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/v1/test/secured"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void securedEndpointAllowsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/test/secured"))
                .andExpect(status().isOk())
                .andExpect(content().string("secured"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void methodSecurityRejectsEmployeeFromSuperAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/test/super"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void methodSecurityAllowsSuperAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/test/super"))
                .andExpect(status().isOk())
                .andExpect(content().string("super"));
    }
}
