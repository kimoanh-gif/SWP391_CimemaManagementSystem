package com.cinema.security;

import com.cinema.dao.*;
import com.cinema.model.*;
import com.cinema.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleLoginFlowTest {
    AuthService auth = mock(AuthService.class);
    AccountDAO accounts = mock(AccountDAO.class);
    RoleDAO roles = mock(RoleDAO.class);
    PermissionDAO permissions = mock(PermissionDAO.class);
    SessionService sessions = new SessionService(roles, permissions);
    OAuth2LoginSuccessHandler handler;
    Account account;
    @BeforeEach void setup() {
        var factory = new DefaultListableBeanFactory(); factory.registerSingleton("auth", auth);
        handler = new OAuth2LoginSuccessHandler(factory.getBeanProvider(AuthService.class), sessions, accounts);
        account = new Account(); account.setAccountId(7); account.setRoleId(3); account.setStatus("ACTIVE");
        account.setFullName("Google Customer"); account.setEmail("customer@example.com"); account.setEmailVerified(true);
        when(roles.findById(3)).thenReturn(Optional.of(new Role(3,"CUSTOMER","Customer")));
        when(permissions.findAllByRoleId(3)).thenReturn(List.of());
    }
    OAuth2AuthenticationToken identity(boolean verified) {
        var oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("google-subject"); when(oidc.getEmail()).thenReturn("customer@example.com");
        when(oidc.getFullName()).thenReturn("Google Customer"); when(oidc.getEmailVerified()).thenReturn(verified);
        return new OAuth2AuthenticationToken(oidc, List.of(), "google");
    }
    @Test void linkedGoogleCustomerGetsFreshSessionAndRedirectsHome() throws Exception {
        when(auth.findAccountByGoogleSubject("google-subject")).thenReturn(Optional.of(account));
        var request = new MockHttpServletRequest(); var old = (MockHttpSession) request.getSession();
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, identity(true));
        assertTrue(old.isInvalid()); assertEquals("/",response.getRedirectedUrl());
        assertEquals(7,sessions.getCurrentUser(request.getSession()).getAccountId());
        assertEquals("OAUTH_GOOGLE",sessions.getCurrentUser(request.getSession()).getAuthMethod());
        verify(accounts).touchLastLogin(7); verify(auth,never()).createGoogleAccount(any(),any(),any(),anyBoolean());
    }
    @Test void newVerifiedGoogleCustomerUsesAccountCreationAndRedirectsHome() throws Exception {
        when(auth.createGoogleAccount("customer@example.com","Google Customer","google-subject",true)).thenReturn(account);
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request,response,identity(true));
        assertEquals("/",response.getRedirectedUrl()); assertEquals("CUSTOMER",sessions.currentRole(request.getSession()));
    }
    @Test void verifiedEmailLinksExistingAccountWithoutCreatingDuplicate() throws Exception {
        when(auth.findAccountByEmail("customer@example.com")).thenReturn(Optional.of(account));
        var request = new MockHttpServletRequest();var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request,response,identity(true));
        verify(auth).linkGoogleIdentity(7,"google-subject","customer@example.com",true);
        verify(auth,never()).createGoogleAccount(any(),any(),any(),anyBoolean());
        assertEquals("/",response.getRedirectedUrl());
    }
    @Test void unverifiedNewIdentityIsRejectedWithoutAccountWrites() throws Exception {
        var request=new MockHttpServletRequest();var response=new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request,response,identity(false));
        assertEquals("/login?oauthRejected",response.getRedirectedUrl());
        verifyNoInteractions(accounts);verify(auth,never()).createGoogleAccount(any(),any(),any(),anyBoolean());
    }
}
