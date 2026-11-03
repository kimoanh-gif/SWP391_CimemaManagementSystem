package com.cinema.controller;

import com.cinema.dao.AccountDAO;
import com.cinema.service.AuthService;
import com.cinema.service.OtpService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Self-service profile/password access does not require any account-management permission. */
@Controller
public class AccountProfileController {
    private final AccountDAO accounts;
    private final SessionService sessions;
    private final AuthService auth;
    private final OtpService otp;

    public AccountProfileController(AccountDAO accounts, SessionService sessions, AuthService auth, OtpService otp) {
        this.accounts=accounts; this.sessions=sessions; this.auth=auth; this.otp=otp;
    }

    @GetMapping("/account/profile")
    public String ownProfile(HttpSession session) {
        var user=sessions.getCurrentUser(session);
        if(user==null) return "redirect:/login";
        if(user.hasRole("ADMIN")) return "redirect:/admin/dashboard";
        return "redirect:/"+user.getRole().toLowerCase(java.util.Locale.ROOT)+"/profile";
    }

    @GetMapping({"/manager/profile","/staff/profile"})
    public String profile(HttpSession session, Model model, jakarta.servlet.http.HttpServletRequest request) {
        var user=sessions.getCurrentUser(session);
        if(user==null) return "redirect:/login";
        if(user.hasRole("ADMIN")) return "redirect:/admin/dashboard";
        String own="/"+user.getRole().toLowerCase(java.util.Locale.ROOT)+"/profile";
        if(!request.getServletPath().equals(own)) return "redirect:"+own;
        var account=accounts.findById(user.getAccountId()).orElse(null);
        if(account==null) return "redirect:/login";
        model.addAttribute("account",account);
        model.addAttribute("roleName",user.getRole());
        model.addAttribute("dashboardUrl",sessions.dashboardForRole(user.getRole()).replaceFirst("^redirect:",""));
        model.addAttribute("passwordMinLength",Math.max(8,auth.passwordMinimumLength()));
        return "account/profile";
    }

    @PostMapping("/account/password/request")
    @ResponseBody
    public ResponseEntity<?> requestOtp(HttpSession session) {
        if(sessions.hasRole(session,"ADMIN")) return error(403,"FORBIDDEN","Personal password changes are not available for Admin.");
        Integer id=sessions.currentAccountId(session);
        if(id==null) return error(401,"UNAUTHENTICATED","Please sign in again.");
        try {
            var result=auth.requestProfilePasswordOtp(id);
            int cooldown=auth.profileOtpCooldownSeconds(id);
            if(!"OK".equals(result.status())) {
                String message=result.message()!=null?result.message():"The verification email could not be sent. Please try again later.";
                return ResponseEntity.status("OTP_LIMIT_EXCEEDED".equals(result.status())?429:400)
                        .body(Map.of("success",false,"code",result.status(),"message",message,"retryAfterSeconds",cooldown));
            }
            return ResponseEntity.ok(Map.of("success",true,"message","A verification code has been sent to your registered email.",
                    "retryAfterSeconds",cooldown,"expiresInSeconds",otp.getOtpTtlSeconds()));
        } catch(org.springframework.dao.DataAccessException ex) {
            return error(503,"UNAVAILABLE","Verification is temporarily unavailable. Please try again.");
        }
    }

    @PostMapping("/account/password/update")
    @ResponseBody
    public ResponseEntity<?> updatePassword(@RequestParam(defaultValue="") String otp,
                                           @RequestParam(defaultValue="") String password,
                                           @RequestParam(defaultValue="") String confirmPassword,
                                           HttpSession session) {
        if(sessions.hasRole(session,"ADMIN")) return error(403,"FORBIDDEN","Personal password changes are not available for Admin.");
        Integer id=sessions.currentAccountId(session);
        if(id==null) return error(401,"UNAUTHENTICATED","Please sign in again.");
        if(!password.equals(confirmPassword)) return error(400,"PASSWORD_MISMATCH","Password confirmation does not match.");
        if(!auth.isPasswordValid(password)) return error(400,"INVALID_PASSWORD",
                "Use "+Math.max(8,auth.passwordMinimumLength())+"–128 characters with at least one letter and one number.");
        if(!otp.trim().matches("[0-9]{6}")) return error(400,"INVALID_OTP","Enter the 6-digit verification code.");
        try {
            var result=auth.updateProfilePassword(id,otp.trim(),password);
            return switch(result) {
                case SUCCESS -> ResponseEntity.ok(Map.of("success",true,"message","Password updated successfully. You are still signed in."));
                case TOO_MANY_ATTEMPTS -> error(400,result.name(),"Too many incorrect attempts. Wait for the OTP lockout period, then request a new code.");
                case NO_PENDING_TOKEN -> error(400,result.name(),"The code has expired or was already used. Request a new code.");
                case INVALID_OR_EXPIRED -> error(400,result.name(),"The verification code is incorrect or no longer valid.");
            };
        } catch(org.springframework.dao.DataAccessException ex) {
            return error(503,"UNAVAILABLE","The password could not be updated. Please try again.");
        }
    }

    private ResponseEntity<?> error(int status,String code,String message) {
        return ResponseEntity.status(status).body(Map.of("success",false,"code",code,"message",message));
    }
}
