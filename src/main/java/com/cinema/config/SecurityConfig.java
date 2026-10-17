package com.cinema.config;

import com.cinema.dao.AccountDAO;
import com.cinema.model.Account;
import com.cinema.security.OAuth2LoginSuccessHandler;
import com.cinema.security.SessionAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Spring Security configuration for the Cinema Management System.
 *
 * <p>Authentication strategy:
 * <ul>
 *   <li>Form login (email/username + password) handled by our own AuthController
 *       – Spring Security is only used for the filter chain, password encoder,
 *       and authorization rules. Custom auth happens via SessionService.</li>
 *   <li>OAuth2 Login with Google (Customer-only). Spring Security takes over the
 *       callback; the success handler hands the verified principal off to our
 *       OAuth2LoginSuccessHandler for account mapping.</li>
 * </ul>
 *
 * <p>Authorization is enforced both in HttpSecurity (URL patterns) and in
 * services/DAOs (defence in depth). CSRF is enabled for browser form posts;
 * the Thymeleaf templates include the {@code _csrf} token.
 */
@Configuration
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
    }

    /**
     * Setter-injected (not constructor) to break the bean creation cycle:
     * SecurityConfig → OAuth2LoginSuccessHandler → AuthService → SecurityConfig.
     * Spring resolves the cycle once the {@code securityFilterChain} bean method
     * runs (after the SecurityConfig instance is fully constructed).
     */
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    /** When true, the OAuth2 client registration is wired. Disable for tests/CI without creds. */
    @Value("${app.auth.oauth.google.enabled:false}")
    private boolean googleOAuthEnabled;

    @Autowired(required = false)
    public void setOAuth2LoginSuccessHandler(OAuth2LoginSuccessHandler handler) {
        this.oAuth2LoginSuccessHandler = handler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 10 – aligned with the OTP and password hash format.
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: enable for browser form posts; the token is exposed as a
            // cookie and rendered into Thymeleaf forms automatically.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                            new AntPathRequestMatcher("/login"),
                            new AntPathRequestMatcher("/register"),
                            new AntPathRequestMatcher("/forgot-password"),
                            new AntPathRequestMatcher("/verify-activation-otp"),
                            new AntPathRequestMatcher("/verify-reset-otp"),
                            new AntPathRequestMatcher("/reset-password"),
                            new AntPathRequestMatcher("/logout"),
                            new AntPathRequestMatcher("/oauth2/**"),
                            new AntPathRequestMatcher("/login/oauth2/**")
                    )
            )
            // Session: keep server-side; default URL rewriting off.
            .sessionManagement(s -> s
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(5)
                    .maxSessionsPreventsLogin(false)
            )
            // Authorisation rules
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.GET,
                        "/", "/home", "/login", "/register",
                        "/forgot-password", "/verify-activation-otp",
                        "/verify-reset-otp", "/reset-password",
                        "/movies", "/movies/**",
                        "/showtimes", "/showtimes/**",
                        "/promotions-news", "/promotions-news/**",
                        "/promotions/**", "/news/**",
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/error", "/favicon.ico",
                        "/oauth2/**", "/login/oauth2/**"
                ).permitAll();

                // Form POSTs are also public (we do our own validation/rate-limit).
                auth.requestMatchers(HttpMethod.POST,
                        "/login", "/register",
                        "/forgot-password", "/verify-activation-otp",
                        "/verify-reset-otp", "/reset-password",
                        "/logout",
                        "/movies/*/reviews"
                ).permitAll();

                // Dashboards are role-gated.
                auth.requestMatchers("/admin/**").hasRole("ADMIN");
                auth.requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER");
                auth.requestMatchers("/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF");
                auth.requestMatchers("/customer/**").hasAnyRole("ADMIN", "MANAGER", "STAFF", "CUSTOMER");

                auth.anyRequest().authenticated();
            })
            // We do our own login/logout forms — Spring Security only provides the filter chain.
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // When an unauthenticated user hits a protected URL → redirect to /login
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        http.addFilterBefore(sessionAuthenticationFilter, AuthorizationFilter.class);

        // Conditionally wire OAuth2 login
        if (googleOAuthEnabled) {
            http.oauth2Login(o -> o.successHandler(oAuth2LoginSuccessHandler));
        }

        return http.build();
    }

    /**
     * On startup, prints BCrypt hashes for the four standard test passwords.
     * Used once to seed the test-account SQL script.
     */
    @Bean
    CommandLineRunner printBcryptHashes(PasswordEncoder pe) {
        return args -> {
            System.out.println("\n╔══════════════════════════════════════════════════╗");
            System.out.println("║   BCrypt hashes for test accounts (copy to SQL) ║");
            System.out.println("╠══════════════════════════════════════════════════╣");
            System.out.printf("║ Admin1234    → %s%n", pe.encode("Admin1234"));
            System.out.printf("║ Manager123   → %s%n", pe.encode("Manager123"));
            System.out.printf("║ Staff1234    → %s%n", pe.encode("Staff1234"));
            System.out.printf("║ Customer123  → %s%n", pe.encode("Customer123"));
            System.out.println("╚══════════════════════════════════════════════════╝\n");
        };
    }

    /**
     * On startup, ensures the four standard test accounts have correct passwords.
     * Overwrites any corrupt/mismatched hash with the known-good BCrypt value.
     * Runs AFTER the PasswordEncoder bean is ready, so it can generate hashes.
     */
    @Bean
    CommandLineRunner seedTestPasswords(PasswordEncoder pe, AccountDAO accountDAO) {
        return args -> {
            accountDAO.findByEmail("admin@cinema.com").ifPresent(acc -> {
                acc.setPasswordHash(pe.encode("Admin1234"));
                accountDAO.updatePasswordHash(acc.getAccountId(), acc.getPasswordHash());
                System.out.println("[CinemaSystem] Reset password hash for admin@cinema.com");
            });
            accountDAO.findByEmail("manager@cinema.com").ifPresent(acc -> {
                acc.setPasswordHash(pe.encode("Manager123"));
                accountDAO.updatePasswordHash(acc.getAccountId(), acc.getPasswordHash());
                System.out.println("[CinemaSystem] Reset password hash for manager@cinema.com");
            });
            accountDAO.findByEmail("staff@cinema.com").ifPresent(acc -> {
                acc.setPasswordHash(pe.encode("Staff1234"));
                accountDAO.updatePasswordHash(acc.getAccountId(), acc.getPasswordHash());
                System.out.println("[CinemaSystem] Reset password hash for staff@cinema.com");
            });
            accountDAO.findByEmail("customer@cinema.com").ifPresent(acc -> {
                acc.setPasswordHash(pe.encode("Customer123"));
                accountDAO.updatePasswordHash(acc.getAccountId(), acc.getPasswordHash());
                System.out.println("[CinemaSystem] Reset password hash for customer@cinema.com");
            });
        };
    }
}
