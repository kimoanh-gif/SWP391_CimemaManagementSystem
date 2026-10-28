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
import org.springframework.context.annotation.Profile;
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

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Spring Security configuration for the Cinema Management System.
 *
 * <p>Authentication strategy:
 * <ul>
 *   <li>Form login (email/username + password) handled by our own AuthController
 *       - Spring Security is only used for the filter chain, password encoder,
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
@EnableWebSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final org.springframework.beans.factory.ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandlerProvider;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter,
                          org.springframework.beans.factory.ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandlerProvider) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.oAuth2LoginSuccessHandlerProvider = oAuth2LoginSuccessHandlerProvider;
    }

    /** When true, the OAuth2 client registration is wired. Disable for tests/CI without creds. */
    @Value("${app.auth.oauth.google.enabled:false}")
    private boolean googleOAuthEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 10 - aligned with the OTP and password hash format.
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
                            "/login",
                            "/register",
                            "/forgot-password",
                            "/verify-activation-otp",
                            "/verify-reset-otp",
                            "/reset-password",
                            "/logout",
                            "/oauth2/**",
                            "/login/oauth2/**",
                            "/manager/**",
                            "/api/**"
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
                        "/oauth2/**", "/login/oauth2/**",
                        "/api/auth/**", "/customer/booking/payment/vnpay/return"
                        , "/ticket/verify/**"
                ).permitAll();

                // Form POSTs are also public (we do our own validation/rate-limit).
                auth.requestMatchers(HttpMethod.POST,
                        "/login", "/register",
                        "/forgot-password", "/verify-activation-otp",
                        "/verify-reset-otp", "/reset-password",
                        "/logout",
                        "/movies/*/reviews",
                        "/api/auth/**"
                ).permitAll();

                // A permission is required in addition to the role boundary.
                auth.requestMatchers("/admin/customers/**", "/admin/staffs/**", "/admin/roles/**",
                        "/api/admin/customers/**", "/api/admin/staffs/**", "/api/admin/roles/**")
                        .access(roleAndPermission("account:manage", "ADMIN"));
                auth.requestMatchers("/admin/dashboard").access(roleAndPermission("report:view", "ADMIN"));
                auth.requestMatchers("/manager", "/manager/dashboard",
                        "/manager/reports/**", "/api/manager/dashboard/**", "/api/manager/reports/**")
                        .access(roleAndPermission("report:view", "MANAGER"));
                auth.requestMatchers("/manager/showtimes/**", "/api/manager/showtimes/**")
                        .access(roleAndPermission("showtime:manage", "MANAGER"));
                auth.requestMatchers("/manager/movies/**", "/api/manager/movies/**")
                        .access(roleAndPermission("movie:manage", "MANAGER"));
                auth.requestMatchers("/manager/genres/**", "/api/manager/genres/**")
                        .access(roleAndPermission("genre:manage", "MANAGER"));
                auth.requestMatchers("/manager/rooms/**", "/api/manager/rooms/**")
                        .access(roleAndPermission("room:manage", "MANAGER"));
                auth.requestMatchers("/manager/seats/**", "/manager/seat-types/**",
                        "/api/manager/seats/**", "/api/manager/seat-types/**")
                        .access(roleAndPermission("seat:manage", "MANAGER"));
                auth.requestMatchers("/manager/pricing/**", "/api/manager/pricing/**")
                        .access(roleAndPermission("pricing:manage", "MANAGER"));
                auth.requestMatchers("/manager/fb/**", "/api/manager/fb/**")
                        .access(roleAndPermission("fb:manage", "MANAGER"));
                auth.requestMatchers("/manager/vouchers/**", "/api/manager/vouchers/**")
                        .access(roleAndPermission("voucher:manage", "MANAGER"));
                auth.requestMatchers("/manager/promotions/**", "/api/manager/promotions/**")
                        .access(roleAndPermission("promotion:manage", "MANAGER"));
                auth.requestMatchers("/manager/news/**", "/api/manager/news/**")
                        .access(roleAndPermission("news:approve", "MANAGER"));
                auth.requestMatchers("/staff/news/**", "/api/staff/news/**")
                        .access(roleAndPermission("news:manage", "ADMIN", "STAFF"));
                auth.requestMatchers("/staff/bookings/**")
                        .access(roleAndPermission("booking:read", "ADMIN", "MANAGER", "STAFF"));
                auth.requestMatchers("/staff/check-in/**")
                        .access(roleAndPermission("ticket:checkin", "ADMIN", "STAFF"));
                auth.requestMatchers("/staff/sales/**", "/api/staff/members/**")
                        .access(roleAndPermission("booking:create", "ADMIN", "STAFF"));
                // Existing payments, tickets and history remain accessible after booking revocation.
                auth.requestMatchers("/customer/booking", "/customer/booking/change-showtime",
                        "/customer/booking/showtime", "/customer/booking/select", "/customer/booking/seats/**",
                        "/customer/booking/foods/**", "/customer/booking/promotions/**", "/customer/booking/checkout")
                        .access(roleAndPermission("booking:create", "CUSTOMER"));

                // Dashboards are role-gated.
                auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                auth.requestMatchers("/api/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF");
                auth.requestMatchers("/manager/**", "/api/manager/**").hasRole("MANAGER");
                auth.requestMatchers("/api/**").permitAll();
                auth.requestMatchers("/admin/**").hasRole("ADMIN");
                auth.requestMatchers("/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF");
                auth.requestMatchers("/customer/**").hasAnyRole("ADMIN", "MANAGER", "STAFF", "CUSTOMER");

                auth.anyRequest().authenticated();
            })
            // We do our own login/logout forms - Spring Security only provides the filter chain.
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // When an unauthenticated user hits a protected URL -> redirect to /login
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        http.addFilterBefore(sessionAuthenticationFilter, AuthorizationFilter.class);

        // Conditionally wire OAuth2 login
        if (googleOAuthEnabled) {
            OAuth2LoginSuccessHandler handler = oAuth2LoginSuccessHandlerProvider.getIfAvailable();
            if (handler != null) {
                http.oauth2Login(o -> o.successHandler(handler));
            }
        }

        return http.build();
    }

    private static org.springframework.security.authorization.AuthorizationManager<org.springframework.security.web.access.intercept.RequestAuthorizationContext>
    roleAndPermission(String permission, String... roles) {
        return (authentication, context) -> {
            var auth = authentication.get();
            boolean hasRole = java.util.Arrays.stream(roles).anyMatch(role -> auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));
            boolean hasPermission = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(permission));
            return new org.springframework.security.authorization.AuthorizationDecision(auth.isAuthenticated() && hasRole && hasPermission);
        };
    }

    /**
     * On startup, prints BCrypt hashes for the four standard test passwords.
     * Used once to seed the test-account SQL script.
     */
    @Bean
    @Profile("dev-password-tools")
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
    @Profile("dev-password-tools")
    CommandLineRunner seedTestPasswords(PasswordEncoder pe, org.springframework.beans.factory.ObjectProvider<AccountDAO> accountDAOProvider) {
        return args -> {
            try {
                AccountDAO accountDAO = accountDAOProvider.getIfAvailable();
                if (accountDAO == null) {
                    return;
                }
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
            } catch (Exception ex) {
                System.out.println("[CinemaSystem] seedTestPasswords skipped: " + ex.getMessage());
            }
        };
    }
}
