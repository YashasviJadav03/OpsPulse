package com.opspulse.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opspulse.common.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that extracts X-Tenant-Id header, places it in TenantContext,
 * and clears the context in the finally block.
 */
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    private final ObjectMapper objectMapper;
    private final boolean enforceOnApiPrefix;

    public TenantFilter() {
        this(true);
    }

    public TenantFilter(boolean enforceOnApiPrefix) {
        this.enforceOnApiPrefix = enforceOnApiPrefix;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantHeader = request.getHeader(TENANT_HEADER);
        String path = request.getRequestURI();

        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                UUID tenantId = UUID.fromString(tenantHeader.trim());
                TenantContext.setTenantId(tenantId);
            } catch (IllegalArgumentException e) {
                writeErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid UUID in X-Tenant-Id header", path);
                return;
            }
        } else if (enforceOnApiPrefix && isTenantRequiredPath(path)) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, "Missing required X-Tenant-Id header", path);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isTenantRequiredPath(String path) {
        if (path == null) {
            return false;
        }
        // Exclude auth endpoints, actuator, public status, docs
        if (path.startsWith("/auth/") || path.startsWith("/actuator") || path.startsWith("/public/")) {
            return false;
        }
        return path.startsWith("/api/");
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), message, path);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
