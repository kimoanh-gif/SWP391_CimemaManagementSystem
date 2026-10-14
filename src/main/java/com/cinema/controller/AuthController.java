package com.cinema.controller;

import com.cinema.model.Account;
import com.cinema.service.AuthService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Single Thymeleaf controller handling every auth flow:
 *   GET  /register                      -> form
 *   POST /register                      -> create + send activation OTP
 *   GET  /verify-activation-otp         -> OTP form
 *   POST /verify-activation-otp         -> activate account
 *   GET  /login / POST /login           -> authenticate (form-based, session-backed)
 *   POST /logout                        -> invalidate session
 *   GET  /forgot-password / POST        -> request reset OTP
 *   GET  /verify-reset-otp / POST       -> verify reset OTP
 *   GET  /reset-password / POST         -> set new password
 */
@Controller
public class AuthController {

    private final AuthService   authService;
    private final SessionService sessionService;

    public AuthController(AuthService authService, SessionService sessionService) {
        this.authService   = authService;
        this.sessionService = sessionService;
    }

    // ============================================================= REGISTER
    @GetMapping("/register")
    public String showRegister(HttpSession session, Model model) {
        if (sessionService.isLoggedIn(session))
            return sessionService.dashboardForRole(sessionService.currentRole(session));
        if (!model.containsAttribute("registerForm"))
            model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("registerForm") RegisterForm form,
                             BindingResult binding, Model model, HttpSession session) {
        if (binding.hasErrors()) {
            binding.getFieldErrors().forEach(e ->
                    model.addAttribute(e.getField() + "Error", e.getDefaultMessage()));
            return "auth/register";
        }

        AuthService.RegisterResult r = authService.register(
                form.getUsername(), form.getEmail(), form.getPassword(),
                form.getFullName(), form.getPhone());

        switch (r) {
            case EMAIL_TAKEN        -> { model.addAttribute("emailError", "Email đã được sử dụng.");     return "auth/register"; }
            case USERNAME_TAKEN     -> { model.addAttribute("usernameError", "Tên đăng nhập đã tồn tại."); return "auth/register"; }
            case INVALID_EMAIL      -> { model.addAttribute("emailError", "Email không hợp lệ.");         return "auth/register"; }
            case INVALID_USERNAME   -> { model.addAttribute("usernameError", "Tên đăng nhập phải 4-50 ký tự, chỉ chứa chữ, số, '.', '_' hoặc '-'."); return "auth/register"; }
            case INVALID_PHONE      -> { model.addAttribute("phoneError", "Số điện thoại không hợp lệ.");return "auth/register"; }
            case INVALID_PASSWORD   -> { model.addAttribute("passwordError", "Mật khẩu phải ≥ 8 ký tự, có chữ và số."); return "auth/register"; }
            case ROLE_NOT_CONFIGURED-> { model.addAttribute("formError", "Hệ thống chưa cấu hình role CUSTOMER."); return "auth/register"; }
            case OTP_DELIVERY_FAILED-> { model.addAttribute("formError", "Đăng ký thành công nhưng gửi email OTP thất bại. Vui lòng thử lại sau."); return "auth/register"; }
            case SUCCESS -> { /* continue */ }
        }

        // Stash the email in session so the OTP page can be reached without re-typing.
        session.setAttribute("activationEmail", form.getEmail().trim().toLowerCase());
        return "redirect:/verify-activation-otp";
    }

    // ====================================================== ACTIVATE (OTP)
    @GetMapping("/verify-activation-otp")
    public String showActivationOtp(HttpSession session, Model model) {
        String email = (String) session.getAttribute("activationEmail");
        if (email == null) return "redirect:/register";
        model.addAttribute("email", email);
        if (!model.containsAttribute("activationForm"))
            model.addAttribute("activationForm", new OtpForm());
        return "auth/verify-activation-otp";
    }

    @PostMapping("/verify-activation-otp")
    public String doActivationOtp(@ModelAttribute("activationForm") OtpForm form,
                                  HttpSession session, Model model) {
        String email = (String) session.getAttribute("activationEmail");
        if (email == null) return "redirect:/register";

        AuthService.OtpResult r = authService.activate(email, form.getOtp());
        switch (r) {
            case SUCCESS -> {
                session.removeAttribute("activationEmail");
                return "redirect:/login?activated";
            }
            case TOO_MANY_ATTEMPTS -> {
                model.addAttribute("formError", "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới.");
                return "auth/verify-activation-otp";
            }
            case NO_PENDING_TOKEN -> {
                session.removeAttribute("activationEmail");
                model.addAttribute("formError", "Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng đăng ký lại.");
                return "auth/verify-activation-otp";
            }
            case INVALID_OR_EXPIRED -> {
                model.addAttribute("otpError", "Mã OTP không đúng hoặc đã hết hạn.");
                model.addAttribute("email", email);
                return "auth/verify-activation-otp";
            }
        }
        return "redirect:/verify-activation-otp";
    }

    // ================================================================ LOGIN
    @GetMapping("/login")
    public String showLogin(HttpSession session, Model model) {
        if (sessionService.isLoggedIn(session))
            return sessionService.dashboardForRole(sessionService.currentRole(session));
        if (!model.containsAttribute("loginForm"))
            model.addAttribute("loginForm", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                          BindingResult binding, Model model, HttpServletRequest request) {
        if (binding.hasErrors()) {
            binding.getFieldErrors().forEach(e ->
                    model.addAttribute(e.getField() + "Error", e.getDefaultMessage()));
            return "auth/login";
        }

        AuthService.LoginResult result = authService.login(form.getIdentifier(), form.getPassword());
        switch (result.status()) {
            case EMAIL_NOT_FOUND -> { model.addAttribute("identifierError", "Tài khoản không tồn tại."); return "auth/login"; }
            case WRONG_PASSWORD  -> { model.addAttribute("passwordError",  "Mật khẩu không chính xác.");  return "auth/login"; }
            case INACTIVE_ACCOUNT -> {
                String reason = "Tài khoản chưa được kích hoạt hoặc đã bị khóa.";
                if (result.account() != null
                        && "PENDING_VERIFICATION".equalsIgnoreCase(result.account().getStatus())) {
                    reason = "Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để nhập OTP.";
                }
                model.addAttribute("formError", reason);
                return "auth/login";
            }
            case UNSUPPORTED_ROLE -> { model.addAttribute("formError", "Vai trò tài khoản chưa được hỗ trợ."); return "auth/login"; }
            case SUCCESS -> { /* continue */ }
        }

        Account account = result.account();
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession fresh = request.getSession(true);
        sessionService.login(fresh, account);
        return sessionService.dashboardForRole(sessionService.currentRole(fresh));
    }

    @PostMapping("/logout")
    public String doLogout(HttpServletRequest request) {
        sessionService.logout(request.getSession(false));
        return "redirect:/login?logout";
    }

    // =========================================================== FORGOT PASSWORD
    @GetMapping("/forgot-password")
    public String showForgot(Model model) {
        if (!model.containsAttribute("forgotForm"))
            model.addAttribute("forgotForm", new ForgotForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String doForgot(@ModelAttribute("forgotForm") ForgotForm form,
                           BindingResult binding, Model model, HttpSession session) {
        if (binding.hasErrors()) {
            binding.getFieldErrors().forEach(e ->
                    model.addAttribute(e.getField() + "Error", e.getDefaultMessage()));
            return "auth/forgot-password";
        }
        boolean ok = authService.requestPasswordReset(form.getEmail());
        if (!ok) {
            model.addAttribute("formError", "Không thể gửi email lúc này. Vui lòng thử lại.");
            return "auth/forgot-password";
        }
        // Save the email so the OTP page can be displayed without re-typing.
        session.setAttribute("resetEmail", form.getEmail().trim().toLowerCase());
        return "redirect:/verify-reset-otp";
    }

    // ====================================================== RESET PASSWORD (OTP)
    @GetMapping("/verify-reset-otp")
    public String showVerifyReset(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot-password";
        model.addAttribute("email", maskEmail(email));
        if (!model.containsAttribute("resetOtpForm"))
            model.addAttribute("resetOtpForm", new OtpForm());
        return "auth/verify-reset-otp";
    }

    @PostMapping("/verify-reset-otp")
    public String doVerifyReset(@ModelAttribute("resetOtpForm") OtpForm form,
                                HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot-password";

        AuthService.OtpResult r = authService.preCheckOtp(email, form.getOtp() == null ? "" : form.getOtp().trim(),
                com.cinema.model.OtpToken.PURPOSE_RESET_PASSWORD);
        switch (r) {
            case SUCCESS -> {
                // Keep the raw OTP in session – the next step will consume it.
                session.setAttribute("resetOtpPending", form.getOtp().trim());
                return "redirect:/reset-password";
            }
            case TOO_MANY_ATTEMPTS -> {
                clearResetSession(session);
                model.addAttribute("formError", "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới.");
                return "auth/verify-reset-otp";
            }
            case NO_PENDING_TOKEN -> {
                clearResetSession(session);
                model.addAttribute("formError", "Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu mã mới.");
                return "auth/verify-reset-otp";
            }
            case INVALID_OR_EXPIRED -> {
                model.addAttribute("otpError", "Mã OTP không đúng hoặc đã hết hạn.");
                model.addAttribute("email", maskEmail(email));
                return "auth/verify-reset-otp";
            }
        }
        return "redirect:/verify-reset-otp";
    }

    @GetMapping("/reset-password")
    public String showReset(HttpSession session, Model model) {
        if (session.getAttribute("resetEmail") == null
                || session.getAttribute("resetOtpPending") == null) {
            return "redirect:/forgot-password";
        }
        if (!model.containsAttribute("resetPasswordForm"))
            model.addAttribute("resetPasswordForm", new ResetPasswordForm());
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String doReset(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                          BindingResult binding, HttpSession session, Model model) {
        if (binding.hasErrors()) {
            binding.getFieldErrors().forEach(e ->
                    model.addAttribute(e.getField() + "Error", e.getDefaultMessage()));
            return "auth/reset-password";
        }
        String email  = (String) session.getAttribute("resetEmail");
        String otp    = (String) session.getAttribute("resetOtpPending");
        if (email == null || otp == null) return "redirect:/forgot-password";

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("confirmPasswordError", "Mật khẩu xác nhận không khớp.");
            return "auth/reset-password";
        }

        AuthService.OtpResult r = authService.resetPassword(email, otp, form.getPassword());
        switch (r) {
            case SUCCESS -> {
                clearResetSession(session);
                return "redirect:/login?passwordResetSuccess";
            }
            case TOO_MANY_ATTEMPTS -> {
                clearResetSession(session);
                model.addAttribute("formError", "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới.");
                return "auth/reset-password";
            }
            case NO_PENDING_TOKEN -> {
                clearResetSession(session);
                model.addAttribute("formError", "Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu mã mới.");
                return "auth/reset-password";
            }
            case INVALID_OR_EXPIRED -> {
                model.addAttribute("formError", "Mã OTP không đúng hoặc đã được sử dụng.");
                return "auth/reset-password";
            }
        }
        return "redirect:/reset-password";
    }

    private void clearResetSession(HttpSession session) {
        session.removeAttribute("resetEmail");
        session.removeAttribute("resetOtpPending");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "email của bạn";
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String visible = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return visible + "***@" + parts[1];
    }

    // ================================================================ Forms
    public static class LoginForm {
        @NotBlank(message = "Vui lòng nhập email hoặc tên đăng nhập.")
        @Size(max = 255)
        private String identifier;

        @NotBlank(message = "Vui lòng nhập mật khẩu.")
        @Size(min = 8, max = 128)
        private String password;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterForm {
        @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
        @Size(min = 4, max = 50, message = "Tên đăng nhập phải có độ dài 4-50 ký tự.")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                 message = "Tên đăng nhập chỉ chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
        private String username;

        @NotBlank(message = "Vui lòng nhập email.")
        @Email(message = "Email không hợp lệ.")
        private String email;

        @NotBlank(message = "Vui lòng nhập mật khẩu.")
        @Size(min = 8, max = 128, message = "Mật khẩu phải có độ dài 8-128 ký tự.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số.")
        private String password;

        @NotBlank(message = "Vui lòng nhập họ tên.")
        @Size(max = 100)
        private String fullName;

        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$",
                 message = "Số điện thoại không hợp lệ.")
        private String phone;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class OtpForm {
        @NotBlank
        @Size(min = 6, max = 6, message = "Mã OTP gồm 6 chữ số.")
        private String otp;
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ForgotForm {
        @NotBlank(message = "Vui lòng nhập email.")
        @Email(message = "Email không hợp lệ.")
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordForm {
        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 8, max = 128, message = "Mật khẩu phải có độ dài 8-128 ký tự.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số.")
        private String password;

        @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
        private String confirmPassword;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
