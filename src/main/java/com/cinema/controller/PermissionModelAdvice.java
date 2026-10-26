package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Uses the authorization snapshot already refreshed by the security filter. */
@ControllerAdvice
public class PermissionModelAdvice {
    @ModelAttribute("permissionUser")
    public AuthenticatedUser permissionUser(HttpServletRequest request) {
        var session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(SessionService.ATTR_USER);
        return user instanceof AuthenticatedUser authenticated ? authenticated : null;
    }
}
