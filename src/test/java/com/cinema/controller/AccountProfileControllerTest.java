package com.cinema.controller;

import com.cinema.dao.AccountDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountProfileControllerTest {
    AccountDAO accounts;SessionService sessions;AuthService auth;OtpService otp;AccountProfileController controller;
    MockHttpSession session;MockMvc mvc;
    @BeforeEach void setup() {
        accounts=mock(AccountDAO.class);sessions=mock(SessionService.class);auth=mock(AuthService.class);otp=mock(OtpService.class);
        controller=new AccountProfileController(accounts,sessions,auth,otp);session=new MockHttpSession();
        when(sessions.currentAccountId(any())).thenReturn(null);
        mvc=MockMvcBuilders.standaloneSetup(controller).build();
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","MANAGER","STAFF","CUSTOMER"})
    void profileRoutesToOwnRole(String role) {
        when(sessions.getCurrentUser(session)).thenReturn(new AuthenticatedUser(42,"user","own@example.com","Own Name",null,role,Set.of(),true,"PASSWORD"));
        assertEquals("redirect:/"+role.toLowerCase()+"/profile",controller.ownProfile(session));
    }
    @Test void unauthenticatedCallsCannotSendOrChange() {
        assertEquals(401,controller.requestOtp(session).getStatusCode().value());
        assertEquals(401,controller.updatePassword("123456","Password1234","Password1234",session).getStatusCode().value());
        verifyNoInteractions(auth,otp);
    }
    @Test void emailSendFailureIsNotShownAsSuccess() {
        when(sessions.currentAccountId(session)).thenReturn(42);when(auth.requestProfilePasswordOtp(42)).thenReturn(AuthService.RegisterResult.otpDeliveryFailed());
        assertEquals(400,controller.requestOtp(session).getStatusCode().value());
    }
    @Test void mismatchedConfirmationDoesNotConsumeOtp() {
        when(sessions.currentAccountId(session)).thenReturn(42);
        assertEquals(400,controller.updatePassword("123456","Password1234","Different1234",session).getStatusCode().value());
        verifyNoInteractions(auth);
    }
    @Test void postCannotSelectAnotherAccountAndKeepsCurrentSession() throws Exception {
        when(sessions.currentAccountId(any())).thenReturn(42);when(auth.isPasswordValid("Password1234")).thenReturn(true);
        when(auth.updateProfilePassword(42,"123456","Password1234")).thenReturn(AuthService.OtpResult.SUCCESS);
        mvc.perform(post("/account/password/update").session(session).param("accountId","99").param("email","other@example.com")
                .param("otp","123456").param("password","Password1234").param("confirmPassword","Password1234"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(auth).updateProfilePassword(42,"123456","Password1234");
        verify(sessions,never()).logout(any());assertFalse(session.isInvalid());
    }
    @Test void missingFieldsAreValidationErrorsNotServerErrors() throws Exception {
        when(sessions.currentAccountId(any())).thenReturn(42);
        mvc.perform(post("/account/password/update").session(session)).andExpect(status().isBadRequest());
        verify(auth,never()).updateProfilePassword(anyInt(),anyString(),anyString());
    }
}
