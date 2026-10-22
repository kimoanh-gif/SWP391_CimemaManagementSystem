package com.cinema.controller;

import com.cinema.model.CustomerProfile;
import com.cinema.dto.ProfileUpdateForm;
import com.cinema.service.CustomerProfileService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final SessionService sessionService;

    public CustomerProfileController(CustomerProfileService customerProfileService,
                                     SessionService sessionService) {
        this.customerProfileService = customerProfileService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String viewProfile(HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        CustomerProfile profile = getProfile(accountId);
        addPageContext(session, model, profile);
        return "customer/profile";
    }

    @GetMapping("/edit")
    public String editProfileForm(HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        CustomerProfile profile = getProfile(accountId);

        addPageContext(session, model, profile);
        model.addAttribute("form", ProfileUpdateForm.from(profile));
        model.addAttribute("latestDateOfBirth", LocalDate.now().minusDays(1));

        return "customer/profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @Valid ProfileUpdateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        CustomerProfile profile = getProfile(accountId);

        if (bindingResult.hasErrors()) {
            addPageContext(session, model, profile);
            model.addAttribute("latestDateOfBirth", LocalDate.now().minusDays(1));
            return "customer/profile-edit";
        }

        try {
            customerProfileService.updateProfile(accountId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("phone", "phone.duplicate", exception.getMessage());
            addPageContext(session, model, profile);
            model.addAttribute("latestDateOfBirth", LocalDate.now().minusDays(1));
            return "customer/profile-edit";
        }
        sessionService.updateProfileSnapshot(session, form.getFullName().trim(), form.getPhone());

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile updated successfully."
        );

        return "redirect:/customer/profile";
    }

    private Integer customerAccountId(HttpSession session) {
        return sessionService.hasRole(session, "CUSTOMER")
                ? sessionService.currentAccountId(session)
                : null;
    }

    private CustomerProfile getProfile(int accountId) {
        try {
            return customerProfileService.getProfile(accountId);
        } catch (IllegalArgumentException exception) {
            throw new ProfileNotFoundException();
        }
    }

    private void addPageContext(HttpSession session, Model model, CustomerProfile profile) {
        model.addAttribute("profile", profile);
        model.addAttribute("currentUser", sessionService.getCurrentUser(session));
        model.addAttribute("availablePoints", profile.getAvailablePoints());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class ProfileNotFoundException extends RuntimeException {
    }
}
