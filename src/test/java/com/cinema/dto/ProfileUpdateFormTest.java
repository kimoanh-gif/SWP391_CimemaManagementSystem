package com.cinema.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileUpdateFormTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void rejectsBlankOrTooShortFullName() {
        ProfileUpdateForm form = validForm();
        form.setFullName("  ");

        assertTrue(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    void acceptsAnEmptyPhoneAndRejectsAnInvalidPhone() {
        ProfileUpdateForm form = validForm();
        form.setPhone(null);
        assertFalse(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("phone")));

        form.setPhone("123");
        assertTrue(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("phone")));
    }

    @Test
    void acceptsPastBirthDateAndRejectsTodayOrFuture() {
        ProfileUpdateForm form = validForm();
        form.setDateOfBirth(LocalDate.now().minusDays(1));
        assertFalse(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateOfBirth")));

        form.setDateOfBirth(LocalDate.now());
        assertTrue(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateOfBirth")));

        form.setDateOfBirth(LocalDate.now().plusDays(1));
        assertTrue(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateOfBirth")));
    }

    @Test
    void rejectsFullNameLongerThanAllowedLimit() {
        ProfileUpdateForm form = validForm();
        form.setFullName("a".repeat(101));

        assertTrue(validator.validate(form).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("fullName")));
    }

    private ProfileUpdateForm validForm() {
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("Customer Example");
        form.setPhone("0901234567");
        form.setDateOfBirth(LocalDate.now().minusYears(20));
        return form;
    }
}
