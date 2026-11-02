package com.cinema.service;

import com.cinema.dao.CustomerAccountDAO;
import com.cinema.model.CustomerAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAccountValidationTest {
    private final CustomerAccountDAO dao = mock(CustomerAccountDAO.class);
    private final AdminPasswordResetService reset = mock(AdminPasswordResetService.class);
    private final CustomerAccountService service = new CustomerAccountService(dao, reset);

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "A"})
    void rejectsInvalidNamesOnCreateAndUpdate(String name) {
        when(dao.findById(42)).thenReturn(Optional.of(new CustomerAccount()));
        assertEquals("INVALID_FULLNAME", service.createAccount("validuser","user@example.com","Password1",name,null).getCode());
        assertEquals("INVALID_FULLNAME", service.updateAccount(42,"validuser","user@example.com",name,null).getCode());
        verify(dao, never()).insertAccount(any(),any(),any(),any(),any());
        verify(dao, never()).updateAccount(anyInt(),any(),any(),any(),any());
    }

    @ParameterizedTest @ValueSource(strings = {"abc", "has space", "invalid@name"})
    void rejectsInvalidUsernames(String username) {
        assertEquals("INVALID_USERNAME",service.createAccount(username,"user@example.com","Password1","Test User",null).getCode());
        verifyNoInteractions(dao);
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"short1", "lettersOnly", "12345678"})
    void rejectsWeakPasswords(String password) {
        assertEquals("INVALID_PASSWORD",service.createAccount("validuser","user@example.com",password,"Test User",null).getCode());
        verify(dao, never()).insertAccount(any(),any(),any(),any(),any());
    }

    @Test void respectsConfiguredPasswordMinimum() {
        SystemSecuritySettings settings = mock(SystemSecuritySettings.class);
        when(settings.read("SYSTEM_PASSWORD_MIN_LENGTH",8)).thenReturn(12);
        service.setSecuritySettings(settings);
        assertEquals("INVALID_PASSWORD",service.createAccount("validuser","user@example.com","Password1","Test User",null).getCode());
        when(dao.insertAccount(any(),any(),any(),any(),isNull())).thenReturn(42);
        assertTrue(service.createAccount("validuser","user+tag@example.com","LongPassword123","Test User",null).isSuccess());
    }

    @Test void missingCustomerCannotBeReadOrMutated() {
        assertTrue(service.getCustomerById(42).isEmpty());
        assertEquals("NOT_FOUND",service.updateAccount(42,"validuser","user@example.com","Test User",null).getCode());
        assertEquals("NOT_FOUND",service.suspendAccount(42).getCode());
        assertEquals("NOT_FOUND",service.reactivateAccount(42).getCode());
        assertEquals("NOT_FOUND",service.resetPassword(42).getCode());
        verifyNoInteractions(reset);
    }
}
