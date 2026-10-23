package com.cinema.controller;

import com.cinema.dto.auth.RegisterForm;
import com.cinema.service.AuthService;
import com.cinema.service.OtpService;
import com.cinema.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistrationControllerTest {
    private AuthService auth;
    private AuthController controller;
    private RegisterForm form;
    private ExtendedModelMap model;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        auth = mock(AuthService.class);
        controller = new AuthController(auth, mock(SessionService.class), mock(OtpService.class));
        form = new RegisterForm();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setFullName("New User");
        form.setPassword("Password1");
        form.setConfirmPassword("Password1");
        model = new ExtendedModelMap();
        session = new MockHttpSession();
    }

    private String submit() {
        return controller.doRegister(form, new BeanPropertyBindingResult(form, "registerForm"), model, session);
    }

    @Test
    void successfulRegistrationStillRedirectsToActivation() {
        when(auth.register(any(), any(), any(), any(), any())).thenReturn(AuthService.RegisterResult.ok());
        assertEquals("redirect:/verify-activation-otp", submit());
        assertEquals("new@example.com", session.getAttribute("activationEmail"));
    }

    @Test
    void deliveryFailureExposesOnlyErrorAndResendOption() {
        when(auth.register(any(), any(), any(), any(), any())).thenReturn(AuthService.RegisterResult.otpDeliveryFailed());
        assertEquals("redirect:/verify-activation-otp", submit());
        assertNull(session.getAttribute("fallbackOtp"));
        // Also discard OTP values that might remain in sessions created by the old code.
        session.setAttribute("fallbackOtp", "123456");
        assertEquals("auth/verify-activation-otp", controller.showActivationOtp(session, model));
        assertNull(session.getAttribute("fallbackOtp"));
        assertFalse(model.containsAttribute("fallbackOtp"));
        assertTrue(model.containsAttribute("formError"));
        assertFalse(model.toString().contains("123456"));
    }

    @ParameterizedTest
    @CsvSource({"USERNAME_TAKEN,usernameError", "EMAIL_TAKEN,emailError", "PHONE_TAKEN,phoneError",
            "REGISTRATION_FAILED,formError"})
    void rolledBackRegistrationReportsCorrectField(String status, String field) {
        when(auth.register(any(), any(), any(), any(), any()))
                .thenThrow(new AuthService.RegistrationFailure(status, null));
        assertEquals("auth/register", submit());
        assertTrue(model.containsAttribute(field));
        assertNull(session.getAttribute("activationEmail"));
    }

    @Test
    void mismatchedPasswordsNeverCallRegistrationService() {
        form.setConfirmPassword("Different1");
        assertEquals("auth/register", submit());
        verifyNoInteractions(auth);
    }

    @Test
    void resendFailureDoesNotExposeOtp() {
        session.setAttribute("activationEmail", "new@example.com");
        when(auth.resendOtp("new@example.com", "ACTIVATION"))
                .thenReturn(new AuthService.ResendResult(false, 0, "OTP_DELIVERY_FAILED", null));
        assertEquals("auth/verify-activation-otp", controller.resendActivationOtp(session, model));
        assertTrue(model.containsAttribute("formError"));
        assertFalse(model.containsAttribute("fallbackOtp"));
    }
}
