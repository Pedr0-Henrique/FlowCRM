package com.flowcrm.shared.web;

import com.flowcrm.shared.security.UserPrincipal;
import org.springframework.dao.DataAccessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);
    private final AuditEventRepository auditEventRepository;

    public AuditLogFilter(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isMutation(request)) {
                Object principal = authenticatedPrincipal();
                if (principal instanceof UserPrincipal user) {
                    record(request, response, user);
                } else if (isPublicAuthMutation(request)) {
                    record(request, response, null);
                }
            }
        }
    }

    private void record(HttpServletRequest request, HttpServletResponse response, UserPrincipal user) {
        UUID userId = user == null ? null : user.getUserId();
        UUID companyId = user == null ? null : user.getCompanyId();
        try {
            auditEventRepository.record(
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    userId,
                    companyId,
                    request.getRemoteAddr()
            );
        } catch (DataAccessException exception) {
            log.error(
                    "event=audit_persistence_failed method={} path={} status={} errorType={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private boolean isMutation(HttpServletRequest request) {
        String method = request.getMethod();
        return request.getRequestURI().startsWith("/api/")
                && ("POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method));
    }

    private boolean isPublicAuthMutation(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        return switch (request.getRequestURI()) {
            case "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh" -> true;
            default -> false;
        };
    }

    private Object authenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getPrincipal();
    }
}
