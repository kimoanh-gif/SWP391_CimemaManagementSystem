package com.cinema.service;

import com.cinema.dao.CustomerProfileDao;
import com.cinema.model.CustomerProfile;
import com.cinema.dto.ProfileUpdateForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private CustomerProfileDao customerProfileDao;

    private CustomerProfileService customerProfileService;

    @BeforeEach
    void setUp() {
        customerProfileService = new CustomerProfileService(customerProfileDao);
        when(customerProfileDao.findByAccountId(1)).thenReturn(Optional.of(new CustomerProfile()));
    }

    @Test
    void rejectsPhoneNumberAlreadyUsedByAnotherAccount() {
        when(customerProfileDao.existsByPhoneExcludingAccount("0901234567", 1)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> customerProfileService.updateProfile(1, validForm()));

        verify(customerProfileDao, never()).updateProfile(eq(1), any(ProfileUpdateForm.class));
    }

    @Test
    void updatesProfileWhenPhoneNumberIsAvailable() {
        when(customerProfileDao.existsByPhoneExcludingAccount("0901234567", 1)).thenReturn(false);

        assertDoesNotThrow(() -> customerProfileService.updateProfile(1, validForm()));

        verify(customerProfileDao).updateProfile(eq(1), any(ProfileUpdateForm.class));
    }

    private ProfileUpdateForm validForm() {
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("Customer Example");
        form.setPhone("0901234567");
        return form;
    }
}
