package com.opspulse.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.common.tenant.TenantFilter;
import com.opspulse.core.domain.Tenant;
import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("isDockerAvailable")
class PostgresTenantIsolationTest {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("opspulse")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

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

        tenantRepository.save(new Tenant(tenantA, "Postgres Tenant A", "ENTERPRISE", Instant.now()));
        tenantRepository.save(new Tenant(tenantB, "Postgres Tenant B", "PRO", Instant.now()));
    }

    @Test
    void shouldIsolateTenantsInPostgres() throws Exception {
        CreateMonitorRequest request = new CreateMonitorRequest(
                "Postgres Monitor",
                "https://postgres.example.com",
                "GET",
                60,
                200,
                true
        );

        mockMvc.perform(post("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Postgres Monitor")));

        // Tenant A sees it
        mockMvc.perform(get("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Tenant B cannot see it
        mockMvc.perform(get("/api/monitors")
                        .header(TenantFilter.TENANT_HEADER, tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
