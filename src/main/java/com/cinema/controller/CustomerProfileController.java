package com.cinema.controller;

import com.cinema.model.CustomerProfile;
import com.cinema.dto.ProfileUpdateForm;
import com.cinema.service.CustomerProfileService;
import com.cinema.service.SessionService;
import com.cinema.service.AuthService;
import com.cinema.model.OtpToken;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final SessionService sessionService;
    private final AuthService authService;

    public CustomerProfileController(CustomerProfileService customerProfileService,
                                     SessionService sessionService,
                                     AuthService authService) {
        this.customerProfileService = customerProfileService;
        this.sessionService = sessionService;
        this.authService = authService;
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

    @GetMapping("/loyalty")
    public String loyalty(HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        CustomerProfile profile = getProfile(accountId);
        addPageContext(session, model, profile);
        return "customer/loyalty";
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
        } catch (CustomerProfileService.DuplicatePhoneException exception) {
            bindingResult.rejectValue("phone", "profile.phone.duplicate", exception.getMessage());
            addPageContext(session, model, profile);
            model.addAttribute("latestDateOfBirth", LocalDate.now().minusDays(1));
            return "customer/profile-edit";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("profile.update", exception.getMessage());
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

    @PostMapping("/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar,
                               HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        CustomerProfile profile = getProfile(accountId);
        if (avatar == null || avatar.isEmpty() || avatar.getSize() > 2 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose a JPG, PNG, or WebP image up to 2 MB.");
            return "redirect:/customer/profile/edit";
        }
        String contentType = avatar.getContentType();
        Map<String,String> extensions = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
        String extension = extensions.get(contentType);
        if (extension == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only JPG, PNG, and WebP images are supported.");
            return "redirect:/customer/profile/edit";
        }
        try {
            Path directory = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = "customer-" + accountId + "-" + UUID.randomUUID() + extension;
            Path target = directory.resolve(fileName).normalize();
            if (!target.startsWith(directory)) throw new IllegalArgumentException("Invalid upload path.");
            Files.copy(avatar.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            customerProfileService.updateAvatar(accountId, "/uploads/avatars/" + fileName);
            deletePreviousAvatar(profile.getAvatarUrl(), directory);
            redirectAttributes.addFlashAttribute("successMessage", "Profile photo updated successfully.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "The profile photo could not be updated.");
        }
        return "redirect:/customer/profile/edit";
    }

    @PostMapping("/password/request")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> requestPasswordChange(HttpSession session) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please sign in again."));
        CustomerProfile profile = getProfile(accountId);
        if (profile.isGoogleLinked() && !profile.isPasswordSet()) {
            return ResponseEntity.ok(Map.of("success", true, "firstGoogleSetup", true,
                    "message", "Create your first password. Email verification is not required this time."));
        }
        authService.requestPasswordReset(profile.getEmail());
        return ResponseEntity.ok(Map.of("success", true, "firstGoogleSetup", false,
                "message", "A verification code has been sent to " + maskEmail(profile.getEmail()) + "."));
    }

    @PostMapping("/password/update")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> updatePassword(@RequestParam(required = false) String otp,
                                                              @RequestParam String newPassword,
                                                              @RequestParam String confirmPassword,
                                                              HttpSession session) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please sign in again."));
        if (!newPassword.equals(confirmPassword)) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password confirmation does not match."));
        CustomerProfile profile = getProfile(accountId);
        boolean success;
        if (profile.isGoogleLinked() && !profile.isPasswordSet()) {
            success = authService.setInitialGooglePassword(accountId, newPassword);
        } else {
            success = authService.resetPassword(profile.getEmail(), otp, newPassword) == AuthService.OtpResult.SUCCESS;
        }
        if (!success) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "The verification code is invalid, expired, or the password does not meet the requirements."));
        return ResponseEntity.ok(Map.of("success", true, "message", "Password updated successfully."));
    }

    private void deletePreviousAvatar(String avatarUrl, Path directory) {
        if (avatarUrl == null || !avatarUrl.startsWith("/uploads/avatars/")) return;
        try {
            Path previous = directory.resolve(avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1)).normalize();
            if (previous.startsWith(directory)) Files.deleteIfExists(previous);
        } catch (Exception ignored) { }
    }

    private String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at < 1) return "your email";
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
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
