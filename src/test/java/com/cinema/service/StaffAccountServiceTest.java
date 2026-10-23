package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffAccountServiceTest {
    private AccountDAO accounts;
    private RoleDAO roles;
    private StaffAccountService service;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountDAO.class);
        roles = mock(RoleDAO.class);
        service = new StaffAccountService(accounts, roles, mock(AdminPasswordResetService.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ADMIN", "admin", "CUSTOMER", "OTHER"})
    void rejectsRolesOutsideStaffAndManager(String role) {
        assertEquals("INVALID_ROLE", create("validuser", "Password1", "Test User", role).getCode());
        verifyNoInteractions(accounts, roles);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "has space", "invalid@name", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"})
    void rejectsInvalidUsernames(String username) {
        assertEquals("INVALID_USERNAME", create(username, "Password1", "Test User", "STAFF").getCode());
        verify(accounts, never()).insert(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "lettersOnly", "12345678"})
    void rejectsWeakPasswords(String password) {
        assertEquals("INVALID_PASSWORD", create("validuser", password, "Test User", "STAFF").getCode());
        verify(accounts, never()).insert(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A"})
    void rejectsInvalidNames(String name) {
        assertEquals("INVALID_FULLNAME", create("validuser", "Password1", name, "STAFF").getCode());
        verify(accounts, never()).insert(any());
    }

    @Test
    void rejectsDuplicatePhone() {
        when(accounts.existsByPhone("0912345678")).thenReturn(true);
        assertEquals("DUPLICATE_PHONE", create("validuser", "Password1", "Test User", "STAFF").getCode());
        verify(accounts, never()).insert(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STAFF", "MANAGER"})
    void acceptsRegistrationFormatsAndHashesPassword(String roleName) {
        Role role = mock(Role.class);
        when(role.getRoleId()).thenReturn(3);
        when(roles.findByName(roleName)).thenReturn(Optional.of(role));
        when(accounts.insert(any())).thenReturn(123);
        var result = service.createAccount("valid.user-1", "user+staff@example.com", "Password1",
                " Test User ", "+84912345678", roleName);
        assertTrue(result.isSuccess());
        var saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).insert(saved.capture());
        assertEquals("Test User", saved.getValue().getFullName());
        assertEquals("+84912345678", saved.getValue().getPhone());
        assertTrue(new BCryptPasswordEncoder().matches("Password1", saved.getValue().getPasswordHash()));
    }

    private StaffAccountService.OperationResult create(String username, String password, String name, String role) {
        return service.createAccount(username, "test@example.com", password, name, "0912345678", role);
    }
}
