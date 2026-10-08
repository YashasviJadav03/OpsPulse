package com.opspulse.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        assertEquals(tenantId, TenantContext.getTenantId());
        assertTrue(TenantContext.getOptionalTenantId().isPresent());
        assertEquals(tenantId, TenantContext.requireTenantId());
    }

    @Test
    void shouldThrowWhenTenantIdRequiredButAbsent() {
        TenantContext.clear();
        assertThrows(IllegalStateException.class, TenantContext::requireTenantId);
        assertNull(TenantContext.getTenantId());
        assertTrue(TenantContext.getOptionalTenantId().isEmpty());
    }

    @Test
    void shouldClearTenantId() {
        TenantContext.setTenantId(UUID.randomUUID());
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }
}
