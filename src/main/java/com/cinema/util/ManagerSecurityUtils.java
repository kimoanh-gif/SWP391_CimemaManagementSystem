package com.cinema.util;

import com.cinema.exception.UnauthorizedManagerException;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility for verifying Manager role authorization on Screen 55 endpoints.
 * Supports session attribute, header, or query parameter checks.
 */
public final class ManagerSecurityUtils {

    private ManagerSecurityUtils() {
    }

    /**
     * Verifies that the current request has Manager privileges.
     * Throws UnauthorizedManagerException (403 Forbidden) if a non-manager role is provided.
     */
    public static void verifyManagerAccess(HttpServletRequest request) {
        String role = resolveRole(request);
        if (role != null && !role.trim().isEmpty()) {
            if (!"MANAGER".equalsIgnoreCase(role.trim()) && !"ADMIN".equalsIgnoreCase(role.trim())) {
                throw new UnauthorizedManagerException(
                        "Truy cập bị từ chối: Tài khoản với vai trò '" + role
                                + "' không có quyền thực hiện thao tác quản lý."
                );
            }
        }
    }

    /**
     * Resolves the current user's role from Session, SecurityContext, Header, or Request Parameter.
     * Never defaults to MANAGER when unauthenticated.
     */
    public static String resolveRole(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 1. Check session (SessionService.ATTR_USER or fallback)
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object authUser = session.getAttribute(SessionService.ATTR_USER);
            if (authUser == null) {
                authUser = session.getAttribute("AUTHENTICATED_USER");
            }
            if (authUser instanceof AuthenticatedUser u && u.getRole() != null) {
                return u.getRole().trim();
            }
            Object sessionRole = session.getAttribute("role");
            if (sessionRole == null) {
                sessionRole = session.getAttribute("userRole");
            }
            if (sessionRole instanceof String str && !str.isBlank()) {
                return str.trim();
            }
        }

        // 2. Check Spring Security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof AuthenticatedUser u && u.getRole() != null) {
                return u.getRole().trim();
            }
            for (var authority : auth.getAuthorities()) {
                String a = authority.getAuthority();
                if (a.startsWith("ROLE_")) {
                    return a.substring(5);
                }
            }
        }

        // 3. Check HTTP Header (used in tests or API clients)
        String headerRole = request.getHeader("X-User-Role");
        if (headerRole != null && !headerRole.isBlank()) {
            return headerRole.trim();
        }

        // 4. Check query parameter (used in tests: ?role=CUSTOMER)
        String paramRole = request.getParameter("role");
        if (paramRole != null && !paramRole.isBlank()) {
            return paramRole.trim();
        }

        // Unauthenticated: never default to MANAGER
        return null;
    }
}
