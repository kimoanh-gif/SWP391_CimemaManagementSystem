package com.cinema.security;

import com.cinema.dao.AccountDAO;
import com.cinema.model.Account;
import com.cinema.service.AuthService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps a Google OIDC login to a local Account.
 * <p>Policy (per PROMPT_AUTH_FEATURES §5):
 * <ol>
 *   <li>If a local account is already linked to the Google {@code sub} → use it directly.</li>
 *   <li>Else if a local account exists with the Google email and the email is
 *       verified by Google → auto-link, mark email verified, set status ACTIVE.</li>
 *   <li>Else if the email is verified by Google and the domain is not
 *       reserved for staff → create a new CUSTOMER account, link it.</li>
 *   <li>Else → reject: the user must sign up with password first.</li>
 * </ol>
 * Internal staff/admin accounts (matching a known internal domain) cannot be
 * created via Google – they must be provisioned by an ADMIN.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * {@link AuthService} is looked up lazily via {@link ObjectProvider} to break
     * the bean creation cycle:
     *   SecurityConfig → OAuth2LoginSuccessHandler → AuthService → SecurityConfig.
     * {@code AuthService} itself only needs the {@code PasswordEncoder} bean, but
     * Spring still treats the indirect reference as a cycle in strict mode. The
     * lazy lookup defers the call until after all singletons are fully initialised.
     */
    private final ObjectProvider<AuthService> authServiceProvider;
    private final SessionService sessionService;
    private final AccountDAO    accountDAO;

    public OAuth2LoginSuccessHandler(ObjectProvider<AuthService> authServiceProvider,
                                     SessionService sessionService,
                                     AccountDAO accountDAO) {
        this.authServiceProvider = authServiceProvider;
        this.sessionService = sessionService;
        this.accountDAO    = accountDAO;
    }

    private AuthService authService() {
        return authServiceProvider.getObject();
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, jakarta.servlet.ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError");
            return;
        }
        Object principalObj = oauthToken.getPrincipal();
        if (!(principalObj instanceof OidcUser oidc)) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError");
            return;
        }
        String subject  = oidc.getSubject();
        String email    = oidc.getEmail();
        boolean verified = Boolean.TRUE.equals(oidc.getEmailVerified());
        String fullName = oidc.getFullName();

        if (subject == null || subject.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError");
            return;
        }

        Account account = resolveAccount(subject, email, fullName, verified);

        if (account == null) {
            response.sendRedirect(request.getContextPath() + "/login?oauthRejected");
            return;
        }

        // Session-fixation defence.
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession fresh = request.getSession(true);

        accountDAO.touchLastLogin(account.getAccountId());
        sessionService.login(fresh, account, SessionService.AUTH_METHOD_OAUTH_GOOGLE);
        response.sendRedirect(request.getContextPath() + sessionService.dashboardForRole(sessionService.currentRole(fresh)));
    }

    private Account resolveAccount(String subject, String email, String fullName, boolean verified) {
        // 1) Direct link by subject
        Optional<Account> byLink = authService().findAccountByGoogleSubject(subject);
        if (byLink.isPresent()) return byLink.get();

        if (email == null || email.isBlank()) return null;
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        // 2) Auto-link by verified email
        Optional<Account> byEmail = authService().findAccountByEmail(normalized);
        if (byEmail.isPresent()) {
            Account existing = byEmail.get();
            if (!verified) return null; // never auto-link on unverified email
            authService().linkGoogleIdentity(existing.getAccountId(), subject, normalized, verified);
            if (!existing.isEmailVerified()) {
                accountDAO.markEmailVerified(existing.getAccountId());
            }
            return existing;
        }

        // 3) Create a new CUSTOMER account (only if Google says email is verified)
        if (!verified) return null;
        return authService().createGoogleAccount(normalized, fullName, subject, true);
    }

}
