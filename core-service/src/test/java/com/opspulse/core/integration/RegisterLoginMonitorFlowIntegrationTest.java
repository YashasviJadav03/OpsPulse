package com.opspulse.core.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.common.tenant.TenantFilter;
import com.opspulse.core.config.SecurityHeaderFilter;
import com.opspulse.core.domain.Role;
import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.CreateUserRequest;
import com.opspulse.core.dto.LoginRequest;
import com.opspulse.core.dto.RegisterTenantRequest;
import com.opspulse.core.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterLoginMonitorFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldExecuteFullRegisterLoginAndMonitorLifecycle() throws Exception {
        // Step 1: Register Tenant + First Admin User
        RegisterTenantRequest registerRequest = new RegisterTenantRequest(
                "Acme Global",
                "ENTERPRISE",
                "admin@acmeglobal.com",
                "AdminPassword123!"
        );

        MvcResult registerResult = mockMvc.perform(post("/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("admin@acmeglobal.com")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String tenantId = registerJson.get("tenantId").asText();
        String initialToken = registerJson.get("token").asText();
        assertThat(initialToken).isNotBlank();

        // Step 2: Login with registered credentials
        LoginRequest loginRequest = new LoginRequest("admin@acmeglobal.com", "AdminPassword123!", null);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("admin@acmeglobal.com")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String loginToken = loginJson.get("token").asText();

        // Validate the JWT claims produced by core-service
        Claims claims = jwtService.extractClaims(loginToken);
        assertThat(claims.get("tenant_id")).isEqualTo(tenantId);
        assertThat(claims.get("role")).isEqualTo("ADMIN");

        // Step 3: Create a Monitor using headers validated/injected by Gateway
        CreateMonitorRequest monitorRequest = new CreateMonitorRequest(
                "Acme Checkout Gateway",
                "https://checkout.acmeglobal.com/health",
                "GET",
                60,
                200,
                true
        );

        MvcResult monitorResult = mockMvc.perform(post("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantId)
                        .header(SecurityHeaderFilter.ROLE_HEADER, claims.get("role", String.class))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(monitorRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Acme Checkout Gateway")))
                .andExpect(jsonPath("$.intervalSeconds", is(60)))
                .andReturn();

        String monitorId = objectMapper.readTree(monitorResult.getResponse().getContentAsString()).get("id").asText();

        // Step 4: List Monitors
        mockMvc.perform(get("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantId)
                        .header(SecurityHeaderFilter.ROLE_HEADER, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(monitorId)));

        // Step 5: Admin creates a Member user
        CreateUserRequest memberRequest = new CreateUserRequest("operator@acmeglobal.com", "OperatorPass123!", Role.MEMBER);
        mockMvc.perform(post("/api/users")
                        .header(TenantFilter.TENANT_HEADER, tenantId)
                        .header(SecurityHeaderFilter.ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("operator@acmeglobal.com")))
                .andExpect(jsonPath("$.role", is("MEMBER")));

        // Step 6: Login as Member
        LoginRequest memberLogin = new LoginRequest("operator@acmeglobal.com", "OperatorPass123!", null);
        MvcResult memberLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("MEMBER")))
                .andReturn();

        JsonNode memberLoginJson = objectMapper.readTree(memberLoginResult.getResponse().getContentAsString());
        Claims memberClaims = jwtService.extractClaims(memberLoginJson.get("token").asText());
        assertThat(memberClaims.get("role")).isEqualTo("MEMBER");

        // Step 7: Member attempts to delete monitor -> 403 Forbidden
        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantId)
                        .header(SecurityHeaderFilter.ROLE_HEADER, memberClaims.get("role", String.class)))
                .andExpect(status().isForbidden());

        // Step 8: Admin deletes monitor -> 204 No Content
        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantId)
                        .header(SecurityHeaderFilter.ROLE_HEADER, "ADMIN"))
                .andExpect(status().isNoContent());
    }
}
