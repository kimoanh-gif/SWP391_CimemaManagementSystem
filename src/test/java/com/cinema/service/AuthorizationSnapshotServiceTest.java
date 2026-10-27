package com.cinema.service;
import com.cinema.dao.*;
import com.cinema.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthorizationSnapshotServiceTest {
    @Test void refreshesPermissionsAndFiltersInvalidCustomerGrantWithoutRelogin() {
        var accounts = mock(AccountDAO.class);
        var roles = mock(RoleDAO.class);
        var permissions = mock(PermissionDAO.class);
        var sessions = new SessionService(roles, permissions);
        var snapshot = new AuthorizationSnapshotService(accounts, sessions);
        var session = new MockHttpSession();
        var account = new Account(); account.setAccountId(1); account.setRoleId(7); account.setStatus("ACTIVE");
        when(accounts.findById(1)).thenReturn(Optional.of(account));
        when(roles.findById(7)).thenReturn(Optional.of(new Role(7, "CUSTOMER", "Customer")));
        when(permissions.findAllByRoleId(7)).thenReturn(List.of(new Permission(1, "booking:create", "Book")));
        sessions.login(session, account, "PASSWORD");
        assertTrue(snapshot.refresh(session).hasPermission("booking:create"));
        when(permissions.findAllByRoleId(7)).thenReturn(List.of(new Permission(2, "account:manage", "Invalid")));
        var refreshed = snapshot.refresh(session);
        assertFalse(refreshed.hasPermission("booking:create"));
        assertFalse(refreshed.hasPermission("account:manage"));
        account.setStatus("SUSPENDED");
        assertNull(snapshot.refresh(session));
        assertTrue(session.isInvalid());
    }
}
