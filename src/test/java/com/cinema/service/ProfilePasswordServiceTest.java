package com.cinema.service;

import com.cinema.dao.*;
import com.cinema.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfilePasswordServiceTest {
    AccountDAO accounts; OtpTokenDAO tokens; OtpService otp; PasswordEncoder encoder; AuthService auth;
    Account account;
    @BeforeEach void setup() {
        accounts=mock(AccountDAO.class); tokens=mock(OtpTokenDAO.class); otp=mock(OtpService.class);encoder=mock(PasswordEncoder.class);
        auth=new AuthService(accounts,mock(RoleDAO.class),mock(CustomerDAO.class),tokens,mock(LoginAttemptDAO.class),mock(OAuthAccountDAO.class),otp,encoder);
        account=new Account();account.setAccountId(42);account.setEmail("own@example.com");account.setStatus("ACTIVE");
        when(accounts.findById(42)).thenReturn(Optional.of(account));
    }
    OtpToken token() {
        var token=new OtpToken();token.setOtpTokenId(7L);token.setAccountId(42);token.setCodeHash("otp-hash");token.setAttemptCount(0);
        when(tokens.findActiveByEmailAndPurpose("own@example.com",OtpToken.PURPOSE_RESET_PASSWORD)).thenReturn(Optional.of(token));
        return token;
    }
    void validOtp() {
        token(); when(otp.verify("123456","otp-hash")).thenReturn(true);
        when(tokens.consumeIfActive(7L)).thenReturn(1);when(encoder.encode("Password1234")).thenReturn("new-hash");
        when(accounts.updatePasswordHash(42,"new-hash")).thenReturn(1);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","MANAGER","STAFF","CUSTOMER"})
    void allRolesCanChangeTheirOwnPassword(String role) {
        account.setRole(role);validOtp();
        assertEquals(AuthService.OtpResult.SUCCESS,auth.updateProfilePassword(42,"123456","Password1234"));
        var order=inOrder(tokens,accounts);
        order.verify(tokens).consumeIfActive(7L);order.verify(accounts).updatePasswordHash(42,"new-hash");
    }
    @Test void googleOnlyAccountStillRequiresOtp() {
        account.setPasswordHash(null);
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.updateProfilePassword(42,null,"Password1234"));
        verify(accounts,never()).updatePasswordHash(anyInt(),anyString());
        validOtp();assertEquals(AuthService.OtpResult.SUCCESS,auth.updateProfilePassword(42,"123456","Password1234"));
    }
    @Test void doesNotAcceptAnotherAccountsTokenAfterAnEmailChange() {
        token().setAccountId(99);
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.updateProfilePassword(42,"123456","Password1234"));
        verify(tokens,never()).consumeIfActive(anyLong());verify(accounts,never()).updatePasswordHash(anyInt(),anyString());
    }
    @Test void expiredOrConsumedCodeCannotChangePassword() {
        assertEquals(AuthService.OtpResult.NO_PENDING_TOKEN,auth.updateProfilePassword(42,"123456","Password1234"));
        verify(accounts,never()).updatePasswordHash(anyInt(),anyString());
    }
    @Test void wrongOtpCountsAttemptWithoutChangingPassword() {
        token();
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.updateProfilePassword(42,"111111","Password1234"));
        verify(tokens).incrementAttempt(7L);verify(accounts,never()).updatePasswordHash(anyInt(),anyString());
    }
    @Test void tooManyAttemptsLocksToken() {
        token().setAttemptCount(4);
        assertEquals(AuthService.OtpResult.TOO_MANY_ATTEMPTS,auth.updateProfilePassword(42,"111111","Password1234"));
        verify(tokens).markConsumed(7L);verify(accounts,never()).updatePasswordHash(anyInt(),anyString());
    }
    @Test void concurrentlyConsumedOtpIsRejected() {
        token();when(otp.verify("123456","otp-hash")).thenReturn(true);
        assertEquals(AuthService.OtpResult.NO_PENDING_TOKEN,auth.updateProfilePassword(42,"123456","Password1234"));
        verify(accounts,never()).updatePasswordHash(anyInt(),anyString());
    }
    @ParameterizedTest @ValueSource(strings={"short1","12345678","lettersOnly"})
    void invalidPasswordsAreRejectedBeforeOtpLookup(String password) {
        assertFalse(auth.isPasswordValid(password));
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.updateProfilePassword(42,"123456",password));
        verifyNoInteractions(tokens);
    }
    @Test void configuredMinimumIsSharedWithForgotPassword() {
        var settings=mock(SystemSecuritySettings.class);when(settings.read("SYSTEM_PASSWORD_MIN_LENGTH",8)).thenReturn(12);auth.setSecuritySettings(settings);
        assertFalse(auth.isPasswordValid("Password1"));assertTrue(auth.isPasswordValid("Password1234"));
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.resetPassword("own@example.com","123456","Password1"));
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.updateProfilePassword(42,"123456","Password1"));
    }
    @Test void sendUsesRegisteredEmailAndSameOtpPurpose() {
        when(otp.generateCode()).thenReturn("123456");when(otp.hash("123456")).thenReturn("otp-hash");
        when(otp.getOtpTtlSeconds()).thenReturn(300);when(otp.sendByEmail("own@example.com","123456",OtpToken.PURPOSE_RESET_PASSWORD)).thenReturn(true);
        assertEquals("OK",auth.requestProfilePasswordOtp(42).status());
        var capture=org.mockito.ArgumentCaptor.forClass(OtpToken.class);verify(tokens).insert(capture.capture());
        assertEquals(42,capture.getValue().getAccountId());assertEquals("own@example.com",capture.getValue().getEmail());
        assertEquals("otp-hash",capture.getValue().getCodeHash());assertTrue(capture.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }
    @Test void sendFailureDoesNotClaimSuccess() {
        when(otp.generateCode()).thenReturn("123456");
        assertEquals("OTP_DELIVERY_FAILED",auth.requestProfilePasswordOtp(42).status());
    }
    @Test void sendRespectsSharedLimitsAndDoesNotInsertAnotherToken() {
        when(otp.canSendOtp("own@example.com",OtpToken.PURPOSE_RESET_PASSWORD)).thenReturn("Wait 60 seconds.");
        assertEquals("OTP_LIMIT_EXCEEDED",auth.requestProfilePasswordOtp(42).status());
        verifyNoInteractions(tokens);verify(otp,never()).sendByEmail(anyString(),anyString(),anyString());
    }
    @Test void inactiveAccountCannotSendOrChangePassword() {
        account.setStatus("SUSPENDED");
        assertEquals("INACTIVE_ACCOUNT",auth.requestProfilePasswordOtp(42).status());
        assertEquals(AuthService.OtpResult.INVALID_OR_EXPIRED,auth.updateProfilePassword(42,"123456","Password1234"));
        verifyNoInteractions(tokens);
    }
    @Test void failedPasswordUpdateThrowsForTransactionRollback() {
        validOtp();when(accounts.updatePasswordHash(42,"new-hash")).thenReturn(0);
        assertThrows(org.springframework.dao.DataAccessException.class,()->auth.updateProfilePassword(42,"123456","Password1234"));
    }

    @Test void profileAndForgotPasswordCannotReuseTheSameOtp() {
        validOtp();when(tokens.consumeIfActive(7L)).thenReturn(1,0);
        assertEquals(AuthService.OtpResult.SUCCESS,auth.updateProfilePassword(42,"123456","Password1234"));
        assertEquals(AuthService.OtpResult.NO_PENDING_TOKEN,auth.resetPassword("own@example.com","123456","Password1234"));
        verify(accounts,times(1)).updatePasswordHash(42,"new-hash");
    }
}
