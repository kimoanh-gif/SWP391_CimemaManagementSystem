package com.cinema.security;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@link SessionService#ATTR_USER} from the HTTP session and populates the
 * Spring Security context for the filter chain.
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

    private final java.util.function.Supplier<com.cinema.service.AuthorizationSnapshotService> snapshots;

    public SessionAuthenticationFilter(com.cinema.service.AuthorizationSnapshotService snapshots) {
        this.snapshots = () -> snapshots;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SessionAuthenticationFilter(org.springframework.beans.factory.ObjectProvider<com.cinema.service.AuthorizationSnapshotService> snapshots) {
        this.snapshots = snapshots::getObject;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        SecurityContextHolder.clearContext();
        Object value = session == null || session.getAttribute(SessionService.ATTR_USER) == null
                ? null : snapshots.get().refresh(session);
        
        if (value instanceof AuthenticatedUser user) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
            if (user.getPermissions() != null) {
                user.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, authorities));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
