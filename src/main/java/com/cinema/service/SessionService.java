package com.cinema.service;

import com.cinema.dao.PermissionDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.Permission;
import com.cinema.model.Role;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Single source of truth for the login session.
 * Views/Controllers never read session attributes directly – they go through this service.
 */
@Service
public class SessionService {

    public static final String ATTR_ACCOUNT_ID    = "loggedInAccountId";
    public static final String ATTR_EMAIL         = "loggedInEmail";
    public static final String ATTR_USERNAME      = "loggedInUsername";
    public static final String ATTR_FULL_NAME     = "loggedInFullName";
    public static final String ATTR_PHONE         = "loggedInPhone";
    public static final String ATTR_ROLE          = "loggedInRole";
    public static final String ATTR_PERMISSIONS   = "loggedInPermissions";

    private final RoleDAO       roleDAO;
    private final PermissionDAO permissionDAO;

    public SessionService(RoleDAO roleDAO, PermissionDAO permissionDAO) {
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
    }

    /**
     * Writes all the attributes required by views / controllers.
     * Note: session-fixation protection is the caller's responsibility – the
     * controller must invalidate the old session before calling this method.
     */
    public void login(HttpSession session, Account account) {
        if (session == null) {
            throw new IllegalArgumentException("HttpSession is required.");
        }
        if (account == null) {
            throw new IllegalArgumentException("Account is required.");
        }
        String roleName = roleDAO.findById(account.getRoleId())
                .map(Role::getName).orElse("CUSTOMER");
        List<String> permissions = permissionDAO.findAllByRoleId(account.getRoleId())
                .stream().map(Permission::getName).toList();

        session.setAttribute(ATTR_ACCOUNT_ID, account.getAccountId());
        session.setAttribute(ATTR_EMAIL,      account.getEmail());
        session.setAttribute(ATTR_USERNAME,   account.getUsername());
        session.setAttribute(ATTR_FULL_NAME,  account.getFullName());
        session.setAttribute(ATTR_PHONE,      account.getPhone());
        session.setAttribute(ATTR_ROLE,       roleName.toUpperCase(Locale.ROOT));
        session.setAttribute(ATTR_PERMISSIONS, permissions);
    }

    public void logout(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(ATTR_ACCOUNT_ID);
        session.removeAttribute(ATTR_EMAIL);
        session.removeAttribute(ATTR_USERNAME);
        session.removeAttribute(ATTR_FULL_NAME);
        session.removeAttribute(ATTR_PHONE);
        session.removeAttribute(ATTR_ROLE);
        session.removeAttribute(ATTR_PERMISSIONS);
        try { session.invalidate(); } catch (IllegalStateException ignored) { }
    }

    public boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(ATTR_ACCOUNT_ID) != null;
    }

    public String currentRole(HttpSession session) {
        if (session == null) return null;
        Object role = session.getAttribute(ATTR_ROLE);
        return role == null ? null : role.toString().toUpperCase(Locale.ROOT);
    }

    public Integer currentAccountId(HttpSession session) {
        if (session == null) return null;
        Object id = session.getAttribute(ATTR_ACCOUNT_ID);
        return (id instanceof Integer i) ? i : null;
    }

    public void updateProfileSnapshot(HttpSession session, String fullName, String phone) {
        if (session == null) return;
        session.setAttribute(ATTR_FULL_NAME, fullName);
        session.setAttribute(ATTR_PHONE, phone);
    }

    public boolean hasRole(HttpSession session, String... allowed) {
        String role = currentRole(session);
        if (role == null) return false;
        for (String r : allowed) {
            if (role.equalsIgnoreCase(r)) return true;
        }
        return false;
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
}
