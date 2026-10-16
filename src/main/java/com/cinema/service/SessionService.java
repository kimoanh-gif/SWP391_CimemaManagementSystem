package com.cinema.service;

import com.cinema.dao.PermissionDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Permission;
import com.cinema.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single source of truth for the login session.
 * Views/Controllers never read session attributes directly – they go through this service.
 */
@Service
public class SessionService {

    public static final String ATTR_USER          = "auth.user";
    public static final String ATTR_CSRF_TOKEN    = "auth.csrf";

    public static final String AUTH_METHOD_PASSWORD    = "PASSWORD";
    public static final String AUTH_METHOD_OAUTH_GOOGLE = "OAUTH_GOOGLE";

    private final RoleDAO       roleDAO;
    private final PermissionDAO permissionDAO;

    public SessionService(RoleDAO roleDAO, PermissionDAO permissionDAO) {
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
    }

    /**
     * Build an AuthenticatedUser from an Account and store it in the session.
     * Caller is responsible for invalidating any prior session (session-fixation defence).
     */
    public void login(HttpSession session, Account account, String authMethod) {
        if (session == null) throw new IllegalArgumentException("HttpSession is required.");
        if (account == null) throw new IllegalArgumentException("Account is required.");

        String roleName = roleDAO.findById(account.getRoleId())
                .map(Role::getName).orElse("CUSTOMER");
        List<Permission> perms = permissionDAO.findAllByRoleId(account.getRoleId());
        Set<String> permNames = new LinkedHashSet<>();
        for (Permission p : perms) permNames.add(p.getName());

        AuthenticatedUser user = new AuthenticatedUser(
                account.getAccountId(),
                account.getUsername(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                roleName == null ? "CUSTOMER" : roleName.toUpperCase(Locale.ROOT),
                permNames,
                account.isEmailVerified(),
                authMethod == null ? AUTH_METHOD_PASSWORD : authMethod);

        session.setAttribute(ATTR_USER, user);
        System.out.println("=== SESSION LOGIN ===");
        System.out.println("sessionId=" + session.getId());
        System.out.println("user=" + user.getUsername() + " role=" + user.getRole());
        System.out.println("session.getAttribute=" + session.getAttribute(ATTR_USER));
    }

    public void logout(HttpSession session) {
        if (session == null) return;
        try { session.removeAttribute(ATTR_USER); } catch (IllegalStateException ignored) {}
        try { session.invalidate(); } catch (IllegalStateException ignored) {}
    }

    public boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session) != null;
    }

    public AuthenticatedUser getCurrentUser(HttpSession session) {
        if (session == null) {
            System.out.println(">>> getCurrentUser: session is NULL");
            return null;
        }
        Object u = session.getAttribute(ATTR_USER);
        System.out.println(">>> getCurrentUser: sessionId=" + session.getId() + ", attr=" + u + ", class=" + (u == null ? "null" : u.getClass().getName()));
        return (u instanceof AuthenticatedUser au) ? au : null;
    }

    public String currentRole(HttpSession session) {
        AuthenticatedUser u = getCurrentUser(session);
        return u == null ? null : u.getRole();
    }

    public Integer currentAccountId(HttpSession session) {
        AuthenticatedUser u = getCurrentUser(session);
        return u == null ? null : u.getAccountId();
    }

    public void updateProfileSnapshot(HttpSession session, String fullName, String phone) {
        if (session == null) return;
        AuthenticatedUser current = getCurrentUser(session);
        if (current == null) return;
        session.setAttribute(ATTR_USER, new AuthenticatedUser(
                current.getAccountId(), current.getUsername(), current.getEmail(),
                fullName, phone, current.getRole(), current.getPermissions(),
                current.isEmailVerified(), current.getAuthMethod()));
    }

    public boolean hasRole(HttpSession session, String... allowed) {
        AuthenticatedUser u = getCurrentUser(session);
        return u != null && u.hasRole(allowed);
    }

    /**
     * Returns the URL the user should land on after login, based on role.
     * If the supplied target is non-null and is a same-app path, it is used.
     */
    public String resolvePostLoginRedirect(HttpSession session, String target) {
        String safe = safeInternalRedirect(target);
        if (safe != null) return "redirect:" + safe;
        return dashboardForRole(currentRole(session));
    }

    public String dashboardForRole(String role) {
        if (role == null) return "redirect:/login";
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "ADMIN"    -> "redirect:/admin/dashboard";
            case "MANAGER"  -> "redirect:/manager/dashboard";
            case "STAFF"    -> "redirect:/staff/dashboard";
            case "CUSTOMER" -> "redirect:/customer/dashboard";
            default         -> "redirect:/login";
        };
    }

    /**
     * Returns the supplied path if it is a same-app relative URL.
     * Prevents open-redirect attacks.
     */
    public static String safeInternalRedirect(String target) {
        if (target == null || target.isBlank()) return null;
        String t = target.trim();
        // Disallow protocol-relative URLs
        if (t.startsWith("//")) return null;
        // Disallow absolute URLs with a scheme (http:, https:, javascript:, data:, etc.)
        if (t.contains("://")) return null;
        // Only allow leading '/' (or empty) and no CRLF
        if (t.indexOf('\n') >= 0 || t.indexOf('\r') >= 0) return null;
        if (!(t.startsWith("/"))) return null;
        // Don't redirect to the login page itself
        if (t.startsWith("/login") || t.startsWith("/logout")) return null;
        return t;
    }

    /** Best-effort client IP (X-Forwarded-For aware). */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
