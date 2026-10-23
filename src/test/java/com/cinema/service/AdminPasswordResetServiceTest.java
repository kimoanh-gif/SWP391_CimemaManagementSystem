package com.cinema.service;

import com.cinema.dao.*;
import com.cinema.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminPasswordResetServiceTest {
    private AccountDAO accounts;
    private JavaMailSender mail;
    private Account account;
    private AdminPasswordResetService service;
    private PlatformTransactionManager transactions;
    private BCryptPasswordEncoder encoder;
    private final AtomicLong now=new AtomicLong(100000);
    private final AtomicReference<SimpleMailMessage> delivered=new AtomicReference<>();
    private final AtomicReference<String> storedHash=new AtomicReference<>();

    @BeforeEach void setup() {
        accounts=mock(AccountDAO.class); mail=mock(JavaMailSender.class);
        encoder=new BCryptPasswordEncoder();
        account=new Account();account.setAccountId(42);account.setRoleId(4);account.setUsername("testuser");
        account.setEmail("registered@example.com");account.setStatus("ACTIVE");
        when(accounts.findByIdForPasswordReset(42)).thenReturn(Optional.of(account));
        when(accounts.updatePasswordHash(eq(42),anyString())).thenAnswer(call->{storedHash.set(call.getArgument(1));return 1;});
        doAnswer(call->{delivered.set(call.getArgument(0));return null;}).when(mail).send(any(SimpleMailMessage.class));
        Clock clock=mock(Clock.class);when(clock.millis()).thenAnswer(call->now.get());
        AdminPasswordResetService target=new AdminPasswordResetService(accounts,mail,encoder,clock);
        transactions=mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenAnswer(call->new SimpleTransactionStatus());
        ProxyFactory proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions,new AnnotationTransactionAttributeSource()));
        service=(AdminPasswordResetService)proxy.getProxy();
    }
    private String emailedPassword() {
        return delivered.get().getText().lines().filter(line->line.startsWith("New password: "))
                .findFirst().orElseThrow().substring("New password: ".length());
    }
    @ParameterizedTest @ValueSource(ints={2,3,4})
    void emailedPasswordMatchesStoredHashAndAllowsLogin(int roleId) {
        account.setRoleId(roleId);
        var result=service.reset(42);
        assertTrue(result.success());
        assertArrayEquals(new String[]{"registered@example.com"},delivered.get().getTo());
        String password=emailedPassword();
        assertTrue(com.cinema.util.AccountValidation.PASSWORD.matcher(password).matches());
        assertTrue(encoder.matches(password,storedHash.get()));
        assertFalse(result.toString().contains(password));
        account.setPasswordHash(storedHash.get());
        when(accounts.findByEmailOrUsername("registered@example.com")).thenReturn(Optional.of(account));
        AuthService auth=new AuthService(accounts,mock(RoleDAO.class),mock(CustomerDAO.class),mock(OtpTokenDAO.class),
                mock(LoginAttemptDAO.class),mock(OAuthAccountDAO.class),mock(OtpService.class),encoder);
        assertEquals(AuthService.LoginResultStatus.SUCCESS,auth.login("registered@example.com",password,"127.0.0.1","test").status());
        verify(transactions).commit(any());
    }
    @Test void smtpFailureRollsBackAndNeverReturnsPassword() {
        doThrow(new MailSendException("SMTP failed")).when(mail).send(any(SimpleMailMessage.class));
        assertThrows(AdminPasswordResetService.DeliveryFailure.class,()->service.reset(42));
        verify(transactions).rollback(any());
        verify(transactions,never()).commit(any());
        assertEquals("RESET_COOLDOWN",service.reset(42).code());
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void repeatedResetIsBlockedUntilSixtySecondsPass() {
        service.reset(42);String first=emailedPassword();
        assertEquals("RESET_COOLDOWN",service.reset(42).code());
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
        now.addAndGet(60000);
        assertTrue(service.reset(42).success());
        verify(mail,times(2)).send(any(SimpleMailMessage.class));
        assertNotEquals(first,emailedPassword());
    }
    @ParameterizedTest @ValueSource(strings={"SUSPENDED","LOCKED","PENDING_VERIFICATION","DISABLED"})
    void inactiveAccountCannotBeResetForLogin(String status) {
        account.setStatus(status);
        assertEquals("INACTIVE_ACCOUNT",service.reset(42).code());
        verify(accounts,never()).updatePasswordHash(anyInt(),anyString());verifyNoInteractions(mail);
    }
    @Test void missingEmailDoesNotChangePassword() {
        account.setEmail("");assertEquals("NO_EMAIL",service.reset(42).code());
        verify(accounts,never()).updatePasswordHash(anyInt(),anyString());verifyNoInteractions(mail);
    }
    @Test void missingAccountDoesNotSendEmail() {
        when(accounts.findByIdForPasswordReset(42)).thenReturn(Optional.empty());
        assertEquals("NOT_FOUND",service.reset(42).code());verifyNoInteractions(mail);
    }
    @Test void databaseFailureDoesNotSendEmail() {
        when(accounts.updatePasswordHash(eq(42),anyString())).thenReturn(0);
        assertThrows(org.springframework.dao.DataAccessException.class,()->service.reset(42));
        verify(transactions).rollback(any());verifyNoInteractions(mail);
    }
}
