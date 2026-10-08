package com.opspulse.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.common.tenant.TenantFilter;
import com.opspulse.core.config.SecurityHeaderFilter;
import com.opspulse.core.domain.Role;
import com.opspulse.core.domain.Tenant;
import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.CreateUserRequest;
import com.opspulse.core.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSecurityRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenantRepository.save(new Tenant(tenantId, "Security Test Tenant", "PRO", Instant.now()));
    }

    @Test
    void shouldAllowAdminAndRejectMemberFromCreatingUsers() throws Exception {
        CreateUserRequest userRequest = new CreateUserRequest("engineer@test.com", "password123", Role.MEMBER);

        // 1. MEMBER attempts to create user -> 403 Forbidden
        mockMvc.perform(post("/api/users")
                        .header(TenantFilter.TENANT_HEADER, tenantId.toString())
                        .header(SecurityHeaderFilter.ROLE_HEADER, "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isForbidden());

        // 2. ADMIN creates user -> 201 Created
        mockMvc.perform(post("/api/users")
                        .header(TenantFilter.TENANT_HEADER, tenantId.toString())
                        .header(SecurityHeaderFilter.ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("engineer@test.com")))
                .andExpect(jsonPath("$.role", is("MEMBER")));
    }

    @Test
    void shouldAllowAdminAndRejectMemberFromDeletingMonitors() throws Exception {
        // Create monitor first
        CreateMonitorRequest monitorRequest = new CreateMonitorRequest(
                "Health Check",
                "https://health.test.com",
                "GET",
                60,
                200,
                true
        );

        MvcResult createResult = mockMvc.perform(post("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(monitorRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String monitorId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 1. MEMBER attempts to delete monitor -> 403 Forbidden
        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantId.toString())
                        .header(SecurityHeaderFilter.ROLE_HEADER, "MEMBER"))
                .andExpect(status().isForbidden());

        // 2. ADMIN deletes monitor -> 204 No Content
        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantId.toString())
                        .header(SecurityHeaderFilter.ROLE_HEADER, "ADMIN"))
                .andExpect(status().isNoContent());
    }
}
