package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.dao.CustomerDAO;
import com.cinema.dao.LoginAttemptDAO;
import com.cinema.dao.OAuthAccountDAO;
import com.cinema.dao.OtpTokenDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService – run with JUnit 5 + Mockito.
 * Mark as NOT RUN in the report if you cannot run a SQL-backed test environment.
 */
class AuthServiceTest {

    private AccountDAO      accountDAO;
    private RoleDAO         roleDAO;
    private CustomerDAO     customerDAO;
    private OtpTokenDAO     otpTokenDAO;
    private LoginAttemptDAO loginAttemptDAO;
    private OAuthAccountDAO oAuthAccountDAO;
    private OtpService      otpService;
    private PasswordEncoder passwordEncoder;
    private AuthService     authService;

    @BeforeEach
    void setUp() {
        accountDAO      = mock(AccountDAO.class);
        roleDAO         = mock(RoleDAO.class);
        customerDAO     = mock(CustomerDAO.class);
        otpTokenDAO     = mock(OtpTokenDAO.class);
        loginAttemptDAO = mock(LoginAttemptDAO.class);
        oAuthAccountDAO = mock(OAuthAccountDAO.class);
        otpService      = mock(OtpService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authService = new AuthService(accountDAO, roleDAO, customerDAO,
                otpTokenDAO, loginAttemptDAO, oAuthAccountDAO,
                otpService, passwordEncoder);
    }

    // ================================================ register validation
    @Test
    void register_rejectsInvalidEmail() {
        var r = authService.register("validuser", "not-an-email", "Password1", "Test User", null);
        assertEquals(AuthService.RegisterResult.invalidEmail(), r);
    }

    @Test
    void register_rejectsShortUsername() {
        var r = authService.register("abc", "ok@example.com", "Password1", "Test User", null);
        assertEquals(AuthService.RegisterResult.invalidUsername(), r);
    }

    @Test
    void register_rejectsWeakPassword() {
        var r = authService.register("validuser", "ok@example.com", "short1", "Test User", null);
        assertEquals(AuthService.RegisterResult.invalidPassword(), r);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(accountDAO.existsByEmail("dup@example.com")).thenReturn(true);
        var r = authService.register("validuser", "dup@example.com", "Password1", "Test User", null);
        assertEquals(AuthService.RegisterResult.emailTaken(), r);
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(accountDAO.existsByEmail("new@example.com")).thenReturn(false);
        when(accountDAO.existsByUsername("taken")).thenReturn(true);
        var r = authService.register("taken", "new@example.com", "Password1", "Test User", null);
        assertEquals(AuthService.RegisterResult.usernameTaken(), r);
    }

    @Test
    void register_succeedsAndSendsActivationOtp() {
        when(accountDAO.existsByEmail(anyString())).thenReturn(false);
        when(accountDAO.existsByUsername(anyString())).thenReturn(false);
        when(roleDAO.findByName("CUSTOMER")).thenReturn(Optional.of(new Role(4, "CUSTOMER", "Customer")));
        when(accountDAO.insert(any(Account.class))).thenReturn(100);
        when(customerDAO.insert(100)).thenReturn(200);
        when(otpService.generateCode()).thenReturn("123456");
        when(otpService.hash(anyString())).thenReturn("$2a$10$hashed");
        when(otpService.sendByEmail(anyString(), anyString(), anyString())).thenReturn(true);

        var r = authService.register("newuser", "new@example.com", "Password1", "Test User", "0901234567");
        assertEquals(AuthService.RegisterResult.ok(), r);
        verify(otpTokenDAO).insert(any());
        verify(otpService).sendByEmail(eq("new@example.com"), eq("123456"), eq("ACTIVATION"));
    }

    @Test
    void register_rejectsInvalidPhone() {
        var r = authService.register("validuser", "ok@example.com", "Password1", "Test User", "abc");
        assertEquals(AuthService.RegisterResult.invalidPhone(), r);
    }

    // ================================================ login
    @Test
    void login_lockedOut_whenTooManyFailuresByIdentifier() {
        when(loginAttemptDAO.countRecentFailuresByIp(any(), anyInt())).thenReturn(0);
        when(loginAttemptDAO.countRecentFailuresByIdentifier(any(), anyInt())).thenReturn(999);

        var r = authService.login("user@example.com", "Password1", "127.0.0.1", "ua");
        assertEquals(AuthService.LoginResultStatus.LOCKED_OUT, r.status());
    }

    @Test
    void login_lockedOut_whenTooManyFailuresByIp() {
        when(loginAttemptDAO.countRecentFailuresByIp(any(), anyInt())).thenReturn(999);

        var r = authService.login("user@example.com", "Password1", "127.0.0.1", "ua");
        assertEquals(AuthService.LoginResultStatus.TOO_MANY_IP_FAILURES, r.status());
    }

    @Test
    void login_returnsEmailNotFoundForUnknownIdentifier() {
        when(loginAttemptDAO.countRecentFailuresByIp(any(), anyInt())).thenReturn(0);
        when(loginAttemptDAO.countRecentFailuresByIdentifier(any(), anyInt())).thenReturn(0);
        when(accountDAO.findByEmailOrUsername(any())).thenReturn(Optional.empty());

        var r = authService.login("nope@example.com", "Password1", "127.0.0.1", "ua");
        assertEquals(AuthService.LoginResultStatus.EMAIL_NOT_FOUND, r.status());
    }

    @Test
    void login_succeedsForValidCredentials() {
        Account acc = new Account();
        acc.setAccountId(1);
        acc.setRoleId(4);
        acc.setEmail("user@example.com");
        acc.setPasswordHash("$2a$10$somehash");
        acc.setStatus("ACTIVE");

        when(loginAttemptDAO.countRecentFailuresByIp(any(), anyInt())).thenReturn(0);
        when(loginAttemptDAO.countRecentFailuresByIdentifier(any(), anyInt())).thenReturn(0);
        when(accountDAO.findByEmailOrUsername(any())).thenReturn(Optional.of(acc));
        when(passwordEncoder.matches("Password1", acc.getPasswordHash())).thenReturn(true);

        var r = authService.login("user@example.com", "Password1", "127.0.0.1", "ua");
        assertEquals(AuthService.LoginResultStatus.SUCCESS, r.status());
        assertEquals(1, r.account().getAccountId());
    }

    @Test
    void login_inactiveAccount() {
        Account acc = new Account();
        acc.setAccountId(1);
        acc.setRoleId(4);
        acc.setEmail("user@example.com");
        acc.setPasswordHash("$2a$10$somehash");
        acc.setStatus("PENDING_VERIFICATION");

        when(loginAttemptDAO.countRecentFailuresByIp(any(), anyInt())).thenReturn(0);
        when(loginAttemptDAO.countRecentFailuresByIdentifier(any(), anyInt())).thenReturn(0);
        when(accountDAO.findByEmailOrUsername(any())).thenReturn(Optional.of(acc));
        when(passwordEncoder.matches("Password1", acc.getPasswordHash())).thenReturn(true);

        var r = authService.login("user@example.com", "Password1", "127.0.0.1", "ua");
        assertEquals(AuthService.LoginResultStatus.INACTIVE_ACCOUNT, r.status());
    }

    // ================================================ safeInternalRedirect
    @Test
    void safeInternalRedirect_blocksExternalUrls() {
        assertNull(SessionService.safeInternalRedirect("https://evil.com"));
        assertNull(SessionService.safeInternalRedirect("//evil.com"));
        assertNull(SessionService.safeInternalRedirect("javascript:alert(1)"));
        assertNull(SessionService.safeInternalRedirect("/login"));
        assertNull(SessionService.safeInternalRedirect("/logout"));
    }

    @Test
    void safeInternalRedirect_allowsInternalPaths() {
        assertEquals("/customer/dashboard", SessionService.safeInternalRedirect("/customer/dashboard"));
        assertEquals("/admin/dashboard",   SessionService.safeInternalRedirect("/admin/dashboard"));
    }
}
