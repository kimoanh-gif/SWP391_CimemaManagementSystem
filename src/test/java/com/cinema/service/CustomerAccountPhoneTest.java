package com.cinema.service;

import com.cinema.dao.CustomerAccountDAO;
import com.cinema.model.CustomerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CustomerAccountPhoneTest {
    private CustomerAccountDAO dao;
    private CustomerAccountService service;
    @BeforeEach void setup() {
        dao=mock(CustomerAccountDAO.class);
        service=new CustomerAccountService(dao,mock(AdminPasswordResetService.class));
        when(dao.findById(42)).thenReturn(Optional.of(new CustomerAccount()));
        when(dao.updateAccount(eq(42),anyString(),nullable(String.class),anyString(),anyString())).thenReturn(1);
    }
    @ParameterizedTest
    @ValueSource(strings={"+84975335058","0975335058","+841234567890","01234567890"})
    void acceptsRegistrationPhoneFormatsWhenEditing(String phone) {
        assertTrue(service.updateAccount(42,"testuser","test@example.com","Test User",phone).isSuccess());
        verify(dao).existsByPhone(phone,42);
        verify(dao).updateAccount(42,"Test User",phone,"test@example.com","testuser");
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","   "})
    void optionalPhoneRemainsOptional(String phone) {
        assertTrue(service.updateAccount(42,"testuser","test@example.com","Test User",phone).isSuccess());
        verify(dao).updateAccount(42,"Test User",null,"test@example.com","testuser");
        verify(dao,never()).existsByPhone(any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"+8497","84975335058","phone","+84975335058000"})
    void rejectsInvalidPhones(String phone) {
        assertEquals("INVALID_PHONE_FORMAT",service.updateAccount(42,"testuser","test@example.com","Test User",phone).getCode());
        verify(dao,never()).updateAccount(anyInt(),any(),any(),any(),any());
    }
    @Test void stillRejectsPhoneOwnedByAnotherAccount() {
        when(dao.existsByPhone("+84975335058",42)).thenReturn(true);
        assertEquals("DUPLICATE_PHONE",service.updateAccount(42,"testuser","test@example.com","Test User","+84975335058").getCode());
        verify(dao,never()).updateAccount(anyInt(),any(),any(),any(),any());
    }
}
