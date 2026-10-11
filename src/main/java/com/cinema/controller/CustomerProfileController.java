package com.cinema.controller;

import com.cinema.model.CustomerProfile;
import com.cinema.model.ProfileUpdateForm;
import com.cinema.service.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping
    public String viewProfile(@RequestParam int accountId, Model model) {
        model.addAttribute("profile", getProfile(accountId));
        return "customer/profile";
    }

    @GetMapping("/edit")
    public String editProfileForm(@RequestParam int accountId, Model model) {
        CustomerProfile profile = getProfile(accountId);

        model.addAttribute("profile", profile);
        model.addAttribute("form", ProfileUpdateForm.from(profile));

        return "customer/profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @RequestParam int accountId,
            @Valid ProfileUpdateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        CustomerProfile profile = getProfile(accountId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", profile);
            return "customer/profile-edit";
        }

        customerProfileService.updateProfile(accountId, form);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile updated successfully."
        );

        return "redirect:/customer/profile?accountId=" + accountId;
    }

    private CustomerProfile getProfile(int accountId) {
        try {
            return customerProfileService.getProfile(accountId);
        } catch (IllegalArgumentException exception) {
            throw new ProfileNotFoundException();
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class ProfileNotFoundException extends RuntimeException {
    }
}