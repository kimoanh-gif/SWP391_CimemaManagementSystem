package com.cinema.util;

import com.cinema.exception.UnauthorizedManagerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Utility for verifying Manager role authorization on Screen 55 endpoints.
 * Supports session attribute, header, or query parameter checks.
 */
public final class ManagerSecurityUtils {

    private ManagerSecurityUtils() {
    }

    /**
     * Verifies that the current request has Manager privileges.
     * Throws UnauthorizedManagerException (403 Forbidden) if another role is provided.
     */
    public static void verifyManagerAccess(HttpServletRequest request) {
        String role = resolveRole(request);
        if (role != null && !role.trim().isEmpty()) {
            if (!"MANAGER".equalsIgnoreCase(role.trim()) && !"ADMIN".equalsIgnoreCase(role.trim())) {
                throw new UnauthorizedManagerException(
                        "Truy cập bị từ chối: Tài khoản với vai trò '" + role
                                + "' không có quyền thực hiện thao tác quản lý thể loại phim."
                );
            }
        }
    }

    /**
     * Resolves the current user's role from Session, Header, or Request Parameter.
     */
    public static String resolveRole(HttpServletRequest request) {
        if (request == null) {
            return "MANAGER";
        }

        // 1. Check session
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object authenticated = session.getAttribute(com.cinema.service.SessionService.ATTR_USER);
            if (authenticated instanceof com.cinema.model.AuthenticatedUser user) {
                return user.getRole();
            }
            Object sessionRole = session.getAttribute("role");
            if (sessionRole == null) {
                sessionRole = session.getAttribute("userRole");
            }
            if (sessionRole instanceof String str && !str.isBlank()) {
                return str.trim();
            }
        }

        // 2. Check HTTP Header
        String headerRole = request.getHeader("X-User-Role");
        if (headerRole != null && !headerRole.isBlank()) {
            return headerRole.trim();
        }

        // 3. Check query parameter
        String paramRole = request.getParameter("role");
        if (paramRole != null && !paramRole.isBlank()) {
            return paramRole.trim();
        }

        // Default: If no authentication session is present (dev environment), assume MANAGER for UI access
        return "MANAGER";
    }
}
