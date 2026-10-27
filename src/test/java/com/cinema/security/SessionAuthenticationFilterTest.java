package com.cinema.security;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cinema.service.AuthorizationSnapshotService;

class SessionAuthenticationFilterTest {

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bridgesCustomSessionUserIntoSpringSecurity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SessionService.ATTR_USER, new AuthenticatedUser(
                9, "customer", "customer@example.com", "Customer", null,
                "CUSTOMER", Set.of("BOOK_TICKETS"), true, SessionService.AUTH_METHOD_PASSWORD));

        var snapshots = mock(AuthorizationSnapshotService.class);
        when(snapshots.refresh(request.getSession())).thenReturn((AuthenticatedUser) request.getSession().getAttribute(SessionService.ATTR_USER));
        new SessionAuthenticationFilter(snapshots).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("BOOK_TICKETS")));
        });
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
