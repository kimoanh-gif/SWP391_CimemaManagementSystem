package com.cinema.controller;

import com.cinema.service.StaffAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StaffAccountApiControllerTest {
    private StaffAccountService service;
    private StaffAccountApiController controller;

    @BeforeEach
    void setUp() {
        service = mock(StaffAccountService.class);
        controller = new StaffAccountApiController();
        ReflectionTestUtils.setField(controller, "service", service);
    }

    @Test
    void missingOrMismatchedConfirmationCannotCreateAccount() {
        var body = new HashMap<>(Map.of("password", "Password1"));
        assertEquals(400, controller.create(body).getStatusCode().value());
        body.put("confirmPassword", "Different1");
        assertEquals(400, controller.create(body).getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test
    void matchingConfirmationCallsValidatedService() {
        when(service.createAccount("staffuser", "staff@example.com", "Password1", "Staff User",
                "0912345678", "STAFF")).thenReturn(StaffAccountService.OperationResult.success("Created"));
        var response = controller.create(Map.of("username", "staffuser", "email", "staff@example.com",
                "password", "Password1", "confirmPassword", "Password1", "fullName", "Staff User",
                "phone", "0912345678", "role", "STAFF"));
        assertEquals(200, response.getStatusCode().value());
        verify(service).createAccount("staffuser", "staff@example.com", "Password1", "Staff User",
                "0912345678", "STAFF");
    }

    @Test
    void resetResponseContainsNoPlaintextPassword() {
        when(service.resetPassword(42)).thenReturn(StaffAccountService.OperationResult.success("Password emailed."));
        var response=controller.resetPassword(42);
        assertEquals(200,response.getStatusCode().value());
        assertEquals(Map.of("success",true,"message","Password emailed."),response.getBody());
    }
}
