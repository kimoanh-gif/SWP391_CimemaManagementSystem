package com.cinema.service;

import com.cinema.dao.*;
import com.cinema.model.*;
import com.cinema.security.SessionAuthenticationFilter;
import org.junit.jupiter.api.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicSecuritySettingsTest {
    @org.junit.jupiter.api.BeforeEach void configurationPermissions() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", "",
                java.util.stream.Stream.of("ROLE_ADMIN", "voucher:configure", "membership:configure", "system:configure")
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList()));
    }
    @org.junit.jupiter.api.AfterEach void clearConfigurationPermissions() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    SystemSettingDAO dao;
    Map<String,SystemSetting> values;
    SystemSecuritySettings policy;
    SystemConfigService admin;
    @BeforeEach void setup() {
        dao = mock(SystemSettingDAO.class);
        values = new LinkedHashMap<>();
        for (var rule : SystemSecuritySettings.RULES) {
            var s = new SystemSetting(); s.setSettingId(values.size()+1); s.setSettingKey(rule.key());
            s.setSettingName(rule.name()); s.setCategory("SYSTEM"); s.setValueType("NUMBER"); s.setIsActive(true);
            s.setValue(String.valueOf(rule.defaultValue())); values.put(rule.key(),s);
        }
        when(dao.findByCategory("SYSTEM")).thenAnswer(i -> new ArrayList<>(values.values()));
        when(dao.findById(anyInt())).thenAnswer(i -> values.values().stream().filter(s -> s.getSettingId().equals(i.getArgument(0))).findFirst());
        when(dao.updateValue(anyInt(),anyString())).thenAnswer(i -> {
            values.values().stream().filter(s -> s.getSettingId().equals(i.getArgument(0))).findFirst().orElseThrow().setValue(i.getArgument(1)); return 1;
        });
        policy = new SystemSecuritySettings(dao);
        admin = new SystemConfigService(dao,mock(MembershipRankDAO.class));
    }
    void save(String key, String value) { assertTrue(admin.updateSetting(values.get(key).getSettingId(),value).success()); }
    AuthService auth(OtpTokenDAO tokens, LoginAttemptDAO attempts, AccountDAO accounts, OtpService otp) {
        var auth = new AuthService(accounts,mock(RoleDAO.class),mock(CustomerDAO.class),tokens,attempts,mock(OAuthAccountDAO.class),otp,mock(PasswordEncoder.class));
        auth.setSecuritySettings(policy); return auth;
    }
    @Test void validatesAllBoundsAndInvalidValuesWithoutWriting() {
        for (var rule : SystemSecuritySettings.RULES) {
            var setting = values.get(rule.key());
            assertTrue(admin.validateSetting(setting,String.valueOf(rule.min())).valid());
            assertTrue(admin.validateSetting(setting,String.valueOf(rule.max())).valid());
            for (String bad : new String[]{"", "-1", "1.5", "1e2", "NaN", "Infinity", " 5 ", "9999999999999", String.valueOf(rule.max()+1), String.valueOf(rule.min()-1)}) {
                assertFalse(admin.updateSetting(setting.getSettingId(),bad).success(),rule.key()+bad);
            }
        }
        verify(dao,never()).updateValue(anyInt(),anyString());
    }
    @Test void otpLifetimeCooldownAndSendLimitsFollowSavedValuesOnSameInstance() {
        var tokens = mock(OtpTokenDAO.class);
        var otp = new OtpService(mock(JavaMailSender.class), tokens); otp.setSecuritySettings(policy);
        assertEquals(300,otp.getOtpTtlSeconds());
        save("SYSTEM_OTP_EXPIRY_MINUTES","2"); assertEquals(120,otp.getOtpTtlSeconds());
        save("SYSTEM_OTP_RESEND_COOLDOWN","120"); assertFalse(otp.canResend(LocalDateTime.now().minusSeconds(90)));
        save("SYSTEM_OTP_RESEND_COOLDOWN","30"); assertTrue(otp.canResend(LocalDateTime.now().minusSeconds(90)));
        when(tokens.getHourlySendCount("u@example.com")).thenReturn(3);
        assertNotNull(otp.canSendOtp("u@example.com","ACTIVATION"));
        save("SYSTEM_OTP_MAX_EMAIL_REQUESTS_PER_HOUR","4"); assertNull(otp.canSendOtp("u@example.com","ACTIVATION"));
        when(tokens.getTodaySendCount("u@example.com","ACTIVATION")).thenReturn(5);
        assertNotNull(otp.canSendOtp("u@example.com","ACTIVATION"));
        save("SYSTEM_OTP_MAX_SEND_DAILY","6"); assertNull(otp.canSendOtp("u@example.com","ACTIVATION"));
        assertEquals(1,otp.getRemainingSends("u@example.com","ACTIVATION"));
    }
    @Test void loginLimitAndPasswordMinimumChangeWithoutRestart() {
        var attempts = mock(LoginAttemptDAO.class); var accounts = mock(AccountDAO.class);
        var auth = auth(mock(OtpTokenDAO.class),attempts,accounts,mock(OtpService.class));
        when(attempts.countRecentFailuresByIdentifier(eq("alice"),anyInt())).thenReturn(2);
        save("SYSTEM_LOGIN_ATTEMPTS","2");
        assertEquals(AuthService.LoginResultStatus.LOCKED_OUT,auth.login("alice","Password1","127.0.0.1","test").status());
        save("SYSTEM_LOGIN_ATTEMPTS","3"); save("SYSTEM_LOGIN_LOCKOUT_MINUTES","10");
        assertEquals(AuthService.LoginResultStatus.EMAIL_NOT_FOUND,auth.login("alice","Password1","127.0.0.1","test").status());
        verify(attempts).countRecentFailuresByIdentifier("alice",10);
        save("SYSTEM_PASSWORD_MIN_LENGTH","12");
        assertEquals("INVALID_PASSWORD",auth.register("alice","u@example.com","Password1","Alice User",null).status());
        assertFalse(auth.changePassword(1,"old","Password1"));
        save("SYSTEM_PASSWORD_MIN_LENGTH","8");
        assertNotEquals("INVALID_PASSWORD",auth.register("alice","u@example.com","Password1","Alice User",null).status());
    }
    @Test void otpAttemptsLockoutAndUnchangedExistingExpiry() {
        var tokens = mock(OtpTokenDAO.class);
        var token = new OtpToken(); token.setOtpTokenId(1L); token.setAttemptCount(1); token.setCodeHash("hash");
        token.setLastSentAt(LocalDateTime.now().minusMinutes(20)); token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        var expiry = token.getExpiresAt();
        when(tokens.findActiveByEmailAndPurpose("u@example.com","ACTIVATION")).thenReturn(Optional.of(token));
        var otp = new OtpService(mock(JavaMailSender.class),tokens); otp.setSecuritySettings(policy);
        var auth = auth(tokens,mock(LoginAttemptDAO.class),mock(AccountDAO.class),otp);
        save("SYSTEM_OTP_MAX_ATTEMPTS","2");
        assertEquals(AuthService.OtpResult.TOO_MANY_ATTEMPTS,auth.preCheckOtp("u@example.com","123456","ACTIVATION"));
        verify(tokens).markConsumed(1L);
        token.setAttemptCount(2); token.setConsumedAt(LocalDateTime.now().minusMinutes(5));
        when(tokens.findLatestByEmailAndPurpose("u@example.com","ACTIVATION")).thenReturn(Optional.of(token));
        save("SYSTEM_OTP_LOCKOUT_MINUTES","10"); assertNotNull(otp.canSendOtp("u@example.com","ACTIVATION"));
        save("SYSTEM_OTP_LOCKOUT_MINUTES","1"); assertNull(otp.canSendOtp("u@example.com","ACTIVATION"));
        save("SYSTEM_OTP_EXPIRY_MINUTES","1"); assertEquals(expiry,token.getExpiresAt());
    }
    @Test void existingSessionsAdoptTheNewIdleTimeout() throws Exception {
        var snapshots = mock(AuthorizationSnapshotService.class);
        var request = new MockHttpServletRequest(); var session = (MockHttpSession)request.getSession();
        var user = new AuthenticatedUser(1,"u","u@example.com","User",null,"CUSTOMER",Set.of(),true,"PASSWORD");
        session.setAttribute(SessionService.ATTR_USER,user); when(snapshots.refresh(session)).thenReturn(user);
        var filter = new SessionAuthenticationFilter(snapshots); filter.setSecuritySettings(policy);
        save("SYSTEM_SESSION_TIMEOUT_MINUTES","10");
        filter.doFilter(request,new MockHttpServletResponse(),(r,s) -> {});
        assertEquals(600,session.getMaxInactiveInterval());
        org.springframework.test.util.ReflectionTestUtils.setField(session,"lastAccessedTime",System.currentTimeMillis()-6*60*1000);
        save("SYSTEM_SESSION_TIMEOUT_MINUTES","5");
        // A fresh filter instance represents the next HTTP request, not an application restart.
        filter = new SessionAuthenticationFilter(snapshots); filter.setSecuritySettings(policy);
        filter.doFilter(request,new MockHttpServletResponse(),(r,s) -> {});
        assertTrue(session.isInvalid());
    }
    @Test void legacyNamesAreResolvedAndDuplicateControlsAreNotDisplayed() {
        var setting = values.get("SYSTEM_OTP_LOCKOUT_MINUTES"); setting.setSettingKey("OTP_LOCKOUT_MINUTES");
        setting.setValue("25"); assertEquals(25,policy.read("SYSTEM_OTP_LOCKOUT_MINUTES",15));
        var duplicate = new SystemSetting(); duplicate.setSettingKey("legacy"); duplicate.setSettingName("OTP Lockout Minutes");
        assertEquals(1,SystemSecuritySettings.editable(List.of(setting,duplicate)).size());
        setting.setValue("NaN"); assertEquals(15,policy.read("SYSTEM_OTP_LOCKOUT_MINUTES",15));
    }

    @Test void adminHttpSaveImmediatelyChangesRuntimePolicy() throws Exception {
        var controller = new com.cinema.controller.SystemConfigApiController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller,"service",admin);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        int id = values.get("SYSTEM_OTP_EXPIRY_MINUTES").getSettingId();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/config/settings/"+id)
                .contentType("application/json").content("{\"value\":\"2\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertEquals(2,policy.read("SYSTEM_OTP_EXPIRY_MINUTES",5));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/config/settings/"+id)
                .contentType("application/json").content("{\"value\":\"1.5\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        assertEquals(2,policy.read("SYSTEM_OTP_EXPIRY_MINUTES",5));
    }

    @Test void newSessionsAndGeneratedAdminPasswordsRespectSavedPolicy() {
        var sessions = new SessionService(mock(RoleDAO.class),mock(PermissionDAO.class));
        sessions.setSecuritySettings(policy);
        var account = new Account(); account.setAccountId(1); account.setRoleId(1);
        save("SYSTEM_SESSION_TIMEOUT_MINUTES","45");
        var session = new MockHttpSession(); sessions.login(session,account,"PASSWORD");
        assertEquals(2700,session.getMaxInactiveInterval());
        var reset = new AdminPasswordResetService(mock(AccountDAO.class),mock(JavaMailSender.class),mock(PasswordEncoder.class));
        reset.setSecuritySettings(policy);
        save("SYSTEM_PASSWORD_MIN_LENGTH","32");
        String password = org.springframework.test.util.ReflectionTestUtils.invokeMethod(reset,"generatePassword");
        assertEquals(32,password.length());
        assertTrue(com.cinema.util.AccountValidation.PASSWORD.matcher(password).matches());
    }

    @Test void newlyIssuedTokensAndEmailUseTheNewLifetimeWhileOldTokensKeepTheirExpiry() {
        var tokens = mock(OtpTokenDAO.class);
        var mail = mock(JavaMailSender.class);
        var otp = new OtpService(mail,tokens); otp.setSecuritySettings(policy);
        var accounts = mock(AccountDAO.class);
        var account = new Account(); account.setAccountId(1); account.setEmail("u@example.com"); account.setStatus("ACTIVE");
        when(accounts.findByEmail("u@example.com")).thenReturn(Optional.of(account));
        when(tokens.insert(any())).thenReturn(1L);
        var auth = auth(tokens,mock(LoginAttemptDAO.class),accounts,otp);
        save("SYSTEM_OTP_EXPIRY_MINUTES","2");
        auth.requestPasswordReset("u@example.com");
        save("SYSTEM_OTP_EXPIRY_MINUTES","9");
        auth.requestPasswordReset("u@example.com");
        var captured = org.mockito.ArgumentCaptor.forClass(OtpToken.class);
        verify(tokens,times(2)).insert(captured.capture());
        assertEquals(120,java.time.Duration.between(captured.getAllValues().get(0).getLastSentAt(),captured.getAllValues().get(0).getExpiresAt()).getSeconds()+1,1);
        assertEquals(540,java.time.Duration.between(captured.getAllValues().get(1).getLastSentAt(),captured.getAllValues().get(1).getExpiresAt()).getSeconds()+1,1);
        var emails = org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
        verify(mail,times(2)).send(emails.capture());
        assertTrue(emails.getAllValues().get(0).getText().contains("2 minutes"));
        assertTrue(emails.getAllValues().get(1).getText().contains("9 minutes"));
    }
}
