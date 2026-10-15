package com.cinema.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {

    @Test
    void safeInternalRedirect_blocksAbsolute() {
        assertNull(SessionService.safeInternalRedirect("https://evil.com"));
        assertNull(SessionService.safeInternalRedirect("http://cinema.local/login"));
    }

    @Test
    void safeInternalRedirect_blocksProtocolRelative() {
        assertNull(SessionService.safeInternalRedirect("//evil.com"));
    }

    @Test
    void safeInternalRedirect_blocksLoginBounce() {
        assertNull(SessionService.safeInternalRedirect("/login?x=1"));
        assertNull(SessionService.safeInternalRedirect("/logout"));
    }

    @Test
    void safeInternalRedirect_blocksNewlines() {
        assertNull(SessionService.safeInternalRedirect("/x\r\nSet-Cookie:"));
        assertNull(SessionService.safeInternalRedirect("/x\nabc"));
    }

    @Test
    void safeInternalRedirect_allowsCleanPaths() {
        assertEquals("/customer/dashboard", SessionService.safeInternalRedirect("/customer/dashboard"));
        assertEquals("/admin/dashboard",   SessionService.safeInternalRedirect("/admin/dashboard"));
        assertEquals("/movies/1",          SessionService.safeInternalRedirect("/movies/1"));
    }

    @Test
    void dashboardForRole_routesCorrectly() {
        SessionService svc = new SessionService(null, null);
        assertEquals("redirect:/admin/dashboard",    svc.dashboardForRole("ADMIN"));
        assertEquals("redirect:/manager/dashboard",  svc.dashboardForRole("MANAGER"));
        assertEquals("redirect:/staff/dashboard",    svc.dashboardForRole("STAFF"));
        assertEquals("redirect:/customer/dashboard", svc.dashboardForRole("CUSTOMER"));
        assertEquals("redirect:/login",              svc.dashboardForRole("GUEST"));
        assertEquals("redirect:/login",              svc.dashboardForRole(null));
    }
}
