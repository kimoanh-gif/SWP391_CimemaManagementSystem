package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.CustomerProfileService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Keeps customer identity details in the shared public header up to date. */
@ControllerAdvice
@ConditionalOnBean({SessionService.class, CustomerProfileService.class})
public class CustomerHeaderAdvice {

    private final SessionService sessionService;
    private final CustomerProfileService customerProfileService;

    public CustomerHeaderAdvice(SessionService sessionService,
                                CustomerProfileService customerProfileService) {
        this.sessionService = sessionService;
        this.customerProfileService = customerProfileService;
    }

    @ModelAttribute("currentAvatarUrl")
    public String currentAvatarUrl(HttpSession session) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        if (user == null || !user.hasRole("CUSTOMER")) return null;
        try {
            return customerProfileService.getProfile(user.getAccountId()).getAvatarUrl();
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
