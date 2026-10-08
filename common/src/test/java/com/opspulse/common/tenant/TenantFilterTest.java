package com.opspulse.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantFilterTest {

    private TenantFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantFilter(true);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExtractTenantIdAndSetInContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/monitors");
        UUID tenantId = UUID.randomUUID();
        request.addHeader(TenantFilter.TENANT_HEADER, tenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<UUID> capturedTenantId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTenantId.set(TenantContext.getTenantId());

        filter.doFilter(request, response, chain);

        assertEquals(tenantId, capturedTenantId.get());
        // Must be cleared in finally block
        assertNull(TenantContext.getTenantId());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectWhenTenantIdIsMissingOnApiEndpoint() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/monitors");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> fail("Filter chain should not be reached");

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing required X-Tenant-Id header"));
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void shouldRejectInvalidUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/monitors");
        request.addHeader(TenantFilter.TENANT_HEADER, "not-a-valid-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> fail("Filter chain should not be reached");

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid UUID in X-Tenant-Id header"));
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void shouldAllowExemptPathsWithoutTenantHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> chainInvoked = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainInvoked.get());
        assertEquals(200, response.getStatus());
        assertNull(TenantContext.getTenantId());
    }
}
