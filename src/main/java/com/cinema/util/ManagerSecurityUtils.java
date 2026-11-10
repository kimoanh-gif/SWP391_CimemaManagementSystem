package com.cinema.util;

import com.cinema.exception.UnauthorizedManagerException;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility for verifying Manager role authorization on Manager endpoints.
 * Resolves identity strictly from trusted server-side SecurityContext or HttpSession.
 */
public final class ManagerSecurityUtils {

    private ManagerSecurityUtils() {
    }

    /**
     * Verifies that the current request has Manager privileges.
     * Throws UnauthorizedManagerException (403 Forbidden) if unauthenticated or another role is provided.
     */
    public static void verifyManagerAccess(HttpServletRequest request) {
        String role = resolveRole(request);
        if (role == null || !"MANAGER".equalsIgnoreCase(role.trim())) {
            throw new UnauthorizedManagerException(
                    "Access denied: Account with role '" + (role != null ? role : "UNAUTHENTICATED")
                            + "' does not have permission to access the manager area."
            );
        }
    }

    /**
     * Verifies Manager access for session-based controllers.
     * - Returns true if the current user has MANAGER role (via SessionService, HttpSession, or SecurityContext).
     * - Throws UnauthorizedManagerException (403 Forbidden) if authenticated with a non-MANAGER role.
     * - Returns false if unauthenticated so the caller can redirect to /login.
     */
    public static boolean verifyOrRedirectManager(HttpSession session, SessionService sessionService) {
        if (sessionService != null && sessionService.hasRole(session, "MANAGER")) {
            return true;
        }
        String role = sessionService != null ? sessionService.currentRole(session) : null;
        if (role == null || role.isBlank()) {
            role = resolveRole(session);
        }
        if ("MANAGER".equalsIgnoreCase(role != null ? role.trim() : null)) {
            return true;
        }
        if (role != null && !role.isBlank()) {
            throw new UnauthorizedManagerException(
                    "Access denied: Account with role '" + role.trim()
                            + "' does not have permission to access the manager area."
            );
        }
        return false;
    }

    /**
     * Resolves the current user's role strictly from SecurityContextHolder or trusted HttpSession attributes.
     * Untrusted HTTP headers, query parameters, and silent default fallbacks are never used.
     */
    public static String resolveRole(HttpServletRequest request) {
        return resolveRole(request != null ? request.getSession(false) : null);
    }

    /**
     * Resolves the current user's role strictly from SecurityContextHolder or trusted HttpSession attributes.
     */
    public static String resolveRole(HttpSession session) {
        // 1. Check trusted Spring Security context first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof AuthenticatedUser authUser
                    && authUser.getRole() != null && !authUser.getRole().isBlank()) {
                return authUser.getRole().trim();
            }
            if (auth.getAuthorities() != null) {
                for (GrantedAuthority authority : auth.getAuthorities()) {
                    String granted = authority.getAuthority();
                    if (granted != null && granted.startsWith("ROLE_") && granted.length() > 5) {
                        return granted.substring(5).trim();
                    }
                }
            }
        }

        // 2. Check trusted server-side HttpSession
        if (session != null) {
            Object authenticated = session.getAttribute(SessionService.ATTR_USER);
            if (authenticated instanceof AuthenticatedUser user
                    && user.getRole() != null && !user.getRole().isBlank()) {
                return user.getRole().trim();
            }
            Object sessionRole = session.getAttribute("role");
            if (sessionRole == null) {
                sessionRole = session.getAttribute("userRole");
            }
            if (sessionRole instanceof String str && !str.isBlank()) {
                return str.trim();
            }
            Object userObj = session.getAttribute("currentUser");
            if (userObj instanceof AuthenticatedUser au
                    && au.getRole() != null && !au.getRole().isBlank()) {
                return au.getRole().trim();
            }
        }

        return null;
    }
}
