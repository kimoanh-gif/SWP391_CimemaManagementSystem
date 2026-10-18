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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                user.getPermissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
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
