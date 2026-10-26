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
    @ValueSource(strings = {"ADMIN", "MANAGER", "STAFF"})
    void includesDatabaseRoleInListSearchAndDetails(String roleName) {
        Account account = new Account();
        account.setAccountId(42);
        account.setRoleId(7);
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(roleName);
        when(roles.findById(7)).thenReturn(Optional.of(role));
        when(accounts.findAll()).thenReturn(java.util.List.of(account));
        when(accounts.searchByKeyword("user")).thenReturn(java.util.List.of(account));
        when(accounts.findById(42)).thenReturn(Optional.of(account));

        assertEquals(roleName, service.searchStaffs(null, 1, 10).get(0).getRole());
        account.setRole(null);
        assertEquals(roleName, service.searchStaffs("user", 1, 10).get(0).getRole());
        account.setRole(null);
        assertEquals(roleName, service.getStaffById(42).orElseThrow().getRole());
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
        if ("STAFF".equals(roleName)) {
            verify(accounts).ensureActiveStaffProfile(123);
        } else {
            verify(accounts, never()).ensureActiveStaffProfile(anyInt());
        }
    }

    private StaffAccountService.OperationResult create(String username, String password, String name, String role) {
        return service.createAccount(username, "test@example.com", password, name, "0912345678", role);
    }

    private void existingStaff() {
        Account account = new Account();
        account.setAccountId(42);
        account.setRoleId(3);
        when(accounts.findById(42)).thenReturn(Optional.of(account));
        when(roles.findById(3)).thenReturn(Optional.of(new Role(3, "STAFF", "Staff")));
        when(roles.findByName("MANAGER")).thenReturn(Optional.of(new Role(2, "MANAGER", "Manager")));
    }

    @Test void editPersistsEveryEditableFieldInOneUpdate() {
        existingStaff();
        when(accounts.updateStaffAccount(42,"new.staff","new@example.com","New Name","+84912345678",2)).thenReturn(1);
        assertTrue(service.updateAccount(42," new.staff "," NEW@example.com "," New Name "," +84912345678 ","MANAGER").isSuccess());
        verify(accounts).updateStaffAccount(42,"new.staff","new@example.com","New Name","+84912345678",2);
        verify(accounts).existsByUsername("new.staff",42);
        verify(accounts).existsByEmail("new@example.com",42);
        verify(accounts).existsByPhone("+84912345678",42);
        verify(accounts,never()).updateRole(anyInt(),anyInt());
        verify(accounts,never()).updateProfile(anyInt(),anyString(),anyString());
        verify(accounts,never()).ensureActiveStaffProfile(anyInt());
    }

    @Test void editRejectsDuplicateEmailWithoutUpdatingAccount() {
        existingStaff();
        when(accounts.existsByEmail("used@example.com",42)).thenReturn(true);
        assertEquals("DUPLICATE_EMAIL",service.updateAccount(42,"new.staff","used@example.com","New Name","0912345678","MANAGER").getCode());
        verify(accounts,never()).updateStaffAccount(anyInt(),anyString(),anyString(),anyString(),anyString(),anyInt());
    }

    @Test void editDoesNotReportSuccessWhenNoRowUpdated() {
        existingStaff();
        assertEquals("UPDATE_FAILED",service.updateAccount(42,"new.staff","new@example.com","New Name","0912345678","MANAGER").getCode());
    }

    @Test void editingStaffRepairsMissingProfileAfterAccountIsSaved() {
        existingStaff();
        when(roles.findByName("STAFF")).thenReturn(Optional.of(new Role(3,"STAFF","Staff")));
        when(accounts.updateStaffAccount(42,"new.staff","new@example.com","New Name","0912345678",3)).thenReturn(1);
        assertTrue(service.updateAccount(42,"new.staff","new@example.com","New Name","0912345678","STAFF").isSuccess());
        var order = inOrder(accounts);
        order.verify(accounts).updateStaffAccount(42,"new.staff","new@example.com","New Name","0912345678",3);
        order.verify(accounts).ensureActiveStaffProfile(42);
    }

    @Test void failedProfileSynchronizationRollsBackAccountTransaction() {
        existingStaff();
        when(roles.findByName("STAFF")).thenReturn(Optional.of(new Role(3,"STAFF","Staff")));
        when(accounts.updateStaffAccount(anyInt(),anyString(),anyString(),anyString(),anyString(),anyInt())).thenReturn(1);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("profile failure"))
                .when(accounts).ensureActiveStaffProfile(42);
        var transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
        var status = mock(org.springframework.transaction.TransactionStatus.class);
        when(transactions.getTransaction(any())).thenReturn(status);
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor(
                transactions, new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.addAdvice(interceptor);
        var transactional = (StaffAccountService) factory.getProxy();
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> transactional.updateAccount(42,"new.staff","new@example.com","New Name","0912345678","STAFF"));
        verify(transactions).rollback(status);
        verify(transactions,never()).commit(any());
    }
}
