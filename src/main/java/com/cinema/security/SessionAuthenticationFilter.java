package com.cinema.security;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@link SessionService#ATTR_USER} from the HTTP session and populates the
 * Spring Security {@link SecurityContext} for the filter chain.
 *
 * <p>Only the thread-local {@link SecurityContextHolder} is used — no
 * {@link org.springframework.security.web.context.SecurityContextRepository}.
 * Controllers access the user via {@link SessionService#getCurrentUser(HttpSession)}
 * injected from the request, which reads the session attribute directly.
 *
 * <p>Registered BEFORE {@code AuthorizationFilter} so that
 * {@code hasRole("ADMIN")} rules work for protected URLs.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SessionService.ATTR_USER);
        
        if (value instanceof AuthenticatedUser user) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
            if (user.getPermissions() != null) {
                user.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, authorities));
        } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String role = ManagerSecurityUtils.resolveRole(request);
            if (role != null && !role.isBlank()) {
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase())
                );
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(role, null, authorities)
                );
            }
        }

        filterChain.doFilter(request, response);
    }
}
