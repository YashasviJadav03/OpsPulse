package com.opspulse.core.config;

import com.opspulse.common.tenant.TenantContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves the tenant identifier from TenantContext for Hibernate 6 @TenantId support.
 * Returns UNKNOWN_TENANT during startup context initialization to prevent SessionFactory bootstrap failure.
 */
@Component
public class HibernateTenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    public static final UUID UNKNOWN_TENANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : UNKNOWN_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return UNKNOWN_TENANT.equals(tenantId);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
