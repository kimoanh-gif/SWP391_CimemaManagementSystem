package com.cinema.service;

import com.cinema.dao.*;
import com.cinema.dto.auth.RegisterForm;
import com.cinema.model.Account;
import com.cinema.model.OtpToken;
import com.cinema.model.Role;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** No application context, SQL connection or SMTP transport is started. */
class RegistrationFlowTest {
    private AccountDAO accounts;
    private CustomerDAO customers;
    private OtpTokenDAO tokens;
    private OtpService otp;
    private AuthService service;
    private PlatformTransactionManager transactions;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountDAO.class);
        customers = mock(CustomerDAO.class);
        tokens = mock(OtpTokenDAO.class);
        otp = mock(OtpService.class);
        RoleDAO roles = mock(RoleDAO.class);
        when(roles.findByName("CUSTOMER")).thenReturn(Optional.of(new Role(4, "CUSTOMER", "Customer")));
        when(accounts.insert(any())).thenReturn(100);
        when(customers.insert(100)).thenReturn(200);
        when(tokens.insert(any())).thenReturn(1L);
        when(otp.generateCode()).thenReturn("123456");
        when(otp.hash("123456")).thenReturn("otp-hash");
        when(otp.sendByEmail(anyString(), anyString(), anyString())).thenReturn(true);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("password-hash");
        AuthService target = new AuthService(accounts, roles, customers, tokens,
                mock(LoginAttemptDAO.class), mock(OAuthAccountDAO.class), otp, encoder);
        transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (AuthService) proxy.getProxy();
    }

    private AuthService.RegisterResult register() {
        return service.register("newuser", "new@example.com", "Password1", "New User", null);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void optionalPhonePassesBeanValidation(String phone) {
        RegisterForm form = new RegisterForm();
        form.setPhone(phone);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validateProperty(form, "phone").isEmpty());
        }
        assertNull(form.getPhone());
    }

    @Test
    void invalidPhoneStillFailsBeanValidation() {
        RegisterForm form = new RegisterForm();
        form.setPhone("invalid");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validateProperty(form, "phone").isEmpty());
        }
    }

    @Test
    void dailyLimitPreventsAnyRegistrationWrites() {
        when(otp.canSendOtp("new@example.com", "ACTIVATION")).thenReturn("Daily limit reached");
        assertEquals("OTP_LIMIT_EXCEEDED", register().status());
        verify(accounts, never()).insert(any());
        verifyNoInteractions(customers, tokens);
        verify(otp, never()).sendByEmail(anyString(), anyString(), anyString());
    }

    @Test
    void customerFailureRollsBackAccountAndDoesNotSendOtp() {
        when(customers.insert(100)).thenThrow(new DataAccessResourceFailureException("DB failure"));
        assertThrows(AuthService.RegistrationFailure.class, this::register);
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
        verifyNoInteractions(tokens);
        verify(otp, never()).sendByEmail(anyString(), anyString(), anyString());
    }

    @Test
    void missingCustomerIdAlsoRollsBack() {
        when(customers.insert(100)).thenReturn(null);
        assertThrows(AuthService.RegistrationFailure.class, this::register);
        verify(transactions).rollback(any());
    }

    @Test
    void limitChangedAfterInsertRollsBackRegistration() {
        when(otp.canSendOtp("new@example.com", "ACTIVATION")).thenReturn(null, "Daily limit reached");
        var error = assertThrows(AuthService.RegistrationFailure.class, this::register);
        assertEquals("OTP_LIMIT_EXCEEDED", error.result().status());
        verify(transactions).rollback(any());
        verifyNoInteractions(tokens);
    }

    @ParameterizedTest
    @CsvSource({"UQ_Account_Username,USERNAME_TAKEN", "UQ_Account_Email,EMAIL_TAKEN",
            "UX_Account_Phone,PHONE_TAKEN", "UnknownConstraint,REGISTRATION_FAILED"})
    void duplicateInsertMapsCorrectFieldAndRollsBack(String constraint, String expected) {
        when(accounts.insert(any())).thenThrow(new DuplicateKeyException("Duplicate index '" + constraint + "'"));
        var error = assertThrows(AuthService.RegistrationFailure.class, this::register);
        assertEquals(expected, error.result().status());
        verify(transactions).rollback(any());
        verifyNoInteractions(customers, tokens);
    }

    @Test
    void deliveryFailureNeverReturnsPlainOtpAndLeavesAccountPendingForResend() {
        when(otp.sendByEmail(anyString(), anyString(), anyString())).thenReturn(false);
        var result = register();
        assertEquals("OTP_DELIVERY_FAILED", result.status());
        assertNull(result.message());
        verify(accounts).insert(argThat(a -> "PENDING_VERIFICATION".equals(a.getStatus())
                && a.getPhone() == null && "password-hash".equals(a.getPasswordHash())));
        verify(tokens).insert(argThat(t -> "otp-hash".equals(t.getCodeHash())));
        verify(transactions).commit(any());
    }

    @Test
    void resendLimitIsNotMisreportedAsDeliveryFailure() {
        OtpToken token = new OtpToken();
        token.setAccountId(100);
        when(tokens.findLatestByEmailAndPurpose("new@example.com", "ACTIVATION")).thenReturn(Optional.of(token));
        when(otp.canResend(any())).thenReturn(true);
        when(otp.canSendOtp("new@example.com", "ACTIVATION")).thenReturn("Daily limit reached");
        var result = service.resendOtp("new@example.com", "ACTIVATION");
        assertEquals("OTP_LIMIT_EXCEEDED", result.code());
        assertEquals("Daily limit reached", result.message());
        verify(tokens, never()).insert(any());
    }

    @Test
    void validRegistrationCommitsBothRecordsAndSendsOtp() {
        assertEquals("OK", register().status());
        verify(accounts).insert(any(Account.class));
        verify(customers).insert(100);
        verify(otp).sendByEmail("new@example.com", "123456", "ACTIVATION");
        verify(transactions).commit(any());
    }
}
