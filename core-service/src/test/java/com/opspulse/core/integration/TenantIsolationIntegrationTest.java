package com.opspulse.core.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.common.tenant.TenantFilter;
import com.opspulse.core.domain.Tenant;
import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.UpdateMonitorRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        tenantRepository.save(new Tenant(tenantA, "Tenant Alpha", "PRO", Instant.now()));
        tenantRepository.save(new Tenant(tenantB, "Tenant Beta", "FREE", Instant.now()));
    }

    @Test
    void shouldEnforceStrictTenantIsolationAcrossMonitors() throws Exception {
        // 1. Tenant A creates a monitor
        CreateMonitorRequest createRequest = new CreateMonitorRequest(
                "Alpha Payment API",
                "https://api.alpha.com/health",
                "GET",
                60,
                200,
                true
        );

        MvcResult createResult = mockMvc.perform(post("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alpha Payment API")))
                .andExpect(jsonPath("$.tenantId", is(tenantA.toString())))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String monitorId = createdJson.get("id").asText();

        // 2. Tenant A can list and get the monitor
        mockMvc.perform(get("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(monitorId)));

        mockMvc.perform(get("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(monitorId)));

        // 3. Tenant B lists monitors -> empty list (cannot see Tenant A's monitors)
        mockMvc.perform(get("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 4. Tenant B gets Tenant A's monitor -> 404 Not Found
        mockMvc.perform(get("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantB.toString()))
                .andExpect(status().isNotFound());

        // 5. Tenant B attempts to update Tenant A's monitor -> 404 Not Found
        UpdateMonitorRequest updateRequest = new UpdateMonitorRequest(
                "Hacked Monitor",
                "https://evil.com",
                "GET",
                60,
                200,
                true
        );
        mockMvc.perform(put("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantB.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        // 6. Tenant B attempts to delete Tenant A's monitor -> 404 Not Found
        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantB.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());

        // 7. Tenant A updates their own monitor -> 200 OK
        UpdateMonitorRequest alphaUpdateRequest = new UpdateMonitorRequest(
                "Alpha Payment API v2",
                "https://api.alpha.com/v2/health",
                "POST",
                120,
                200,
                true
        );
        mockMvc.perform(put("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alphaUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alpha Payment API v2")))
                .andExpect(jsonPath("$.intervalSeconds", is(120)));

        // 8. Tenant A deletes their monitor -> 204 No Content
        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        // 9. Tenant A attempts to get deleted monitor -> 404 Not Found
        mockMvc.perform(get("/api/monitors/" + monitorId)
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectRequestWithoutTenantContext() throws Exception {
        mockMvc.perform(get("/api/monitors"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Missing required X-Tenant-Id header")));
    }

    @Test
    void shouldRejectRequestWithInvalidUuidTenantHeader() throws Exception {
        mockMvc.perform(get("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid UUID in X-Tenant-Id header")));
    }

    @Test
    void shouldRejectInvalidMonitorInterval() throws Exception {
        CreateMonitorRequest invalidRequest = new CreateMonitorRequest(
                "Short interval",
                "https://api.example.com",
                "GET",
                10, // Invalid: interval must be >= 30 seconds
                200,
                true
        );

        mockMvc.perform(post("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
