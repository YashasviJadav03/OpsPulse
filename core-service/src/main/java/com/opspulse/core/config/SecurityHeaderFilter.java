package com.opspulse.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extracts X-User-Role and X-User-Id headers injected by the API Gateway
 * and populates the Spring Security SecurityContext.
 */
public class SecurityHeaderFilter extends OncePerRequestFilter {

    public static final String ROLE_HEADER = "X-User-Role";
    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String roleHeader = request.getHeader(ROLE_HEADER);
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (roleHeader != null && !roleHeader.isBlank()) {
            String roleName = roleHeader.trim().toUpperCase();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            String principal = (userIdHeader != null && !userIdHeader.isBlank())
                    ? userIdHeader.trim()
                    : "authenticated-user";

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(roleName)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
