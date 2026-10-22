package com.cinema.controller;

import com.cinema.dto.auth.ForgotPasswordForm;
import com.cinema.dto.auth.LoginForm;
import com.cinema.dto.auth.OtpForm;
import com.cinema.dto.auth.RegisterForm;
import com.cinema.dto.auth.ResetPasswordForm;
import com.cinema.model.Account;
import com.cinema.service.AuthService;
import com.cinema.service.OtpService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single Thymeleaf controller handling every auth flow:
 * <ul>
 *   <li>GET/POST /register, /verify-activation-otp</li>
 *   <li>GET/POST /login, /logout</li>
 *   <li>GET/POST /forgot-password, /verify-reset-otp, /reset-password</li>
 * </ul>
 * Spring Security handles the OAuth2 /login/oauth2/... endpoints.
 */
@Controller
public class AuthController {

    private final AuthService   authService;
    private final SessionService sessionService;
    private final OtpService    otpService;

    @Value("${app.auth.oauth.google.enabled:false}")
    private boolean googleOAuthEnabled;

    public AuthController(AuthService authService, SessionService sessionService, OtpService otpService) {
        this.authService   = authService;
        this.sessionService = sessionService;
        this.otpService    = otpService;
    }

    @ModelAttribute("googleOAuthEnabled")
    public boolean googleOAuthEnabled() {
        return googleOAuthEnabled;
    }

    // Home route is handled by HomeController (Screen 21 - Home Page).

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

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("confirmPasswordError", "Mật khẩu xác nhận không khớp.");
            return "auth/register";
        }

        AuthService.RegisterResult r = authService.register(
                form.getUsername(), form.getEmail(), form.getPassword(),
                form.getFullName(), form.getPhone());
        String rawOtp = r.rawOtp();

        String status = r.status();
        switch (status) {
            case "EMAIL_TAKEN"        -> { model.addAttribute("emailError", "Email đã được sử dụng."); return "auth/register"; }
            case "USERNAME_TAKEN"     -> { model.addAttribute("usernameError", "Tên đăng nhập đã tồn tại."); return "auth/register"; }
            case "PHONE_TAKEN"        -> { model.addAttribute("phoneError", "Số điện thoại đã được sử dụng."); return "auth/register"; }
            case "INVALID_EMAIL"      -> { model.addAttribute("emailError", "Email không hợp lệ."); return "auth/register"; }
            case "INVALID_USERNAME"   -> { model.addAttribute("usernameError", "Tên đăng nhập phải 4-50 ký tự, chỉ chứa chữ, số, '.', '_' hoặc '-'."); return "auth/register"; }
            case "INVALID_PHONE"      -> { model.addAttribute("phoneError", "Số điện thoại không hợp lệ."); return "auth/register"; }
            case "INVALID_PASSWORD"   -> { model.addAttribute("passwordError", "Mật khẩu phải ≥ 8 ký tự, có chữ và số."); return "auth/register"; }
            case "INVALID_FULLNAME"  -> { model.addAttribute("fullNameError", "Họ tên không hợp lệ (2-100 ký tự)."); return "auth/register"; }
            case "ROLE_NOT_CONFIGURED" -> { model.addAttribute("formError", "Hệ thống chưa cấu hình role CUSTOMER."); return "auth/register"; }
            case "OTP_DELIVERY_FAILED" -> {
                // Email failed — store OTP in session so the page can display it.
                session.setAttribute("activationEmail", form.getEmail().trim().toLowerCase());
                session.setAttribute("fallbackOtp", rawOtp);
                return "redirect:/verify-activation-otp";
            }
            case "OTP_LIMIT_EXCEEDED" -> {
                // Daily send limit exceeded — show error message.
                model.addAttribute("formError", rawOtp != null ? rawOtp : "Đã gửi tối đa mã OTP trong ngày. Vui lòng thử lại vào ngày mai.");
                return "auth/register";
            }
            case "OK" -> { /* continue */ }
        }

        session.setAttribute("activationEmail", form.getEmail().trim().toLowerCase());
        return "redirect:/verify-activation-otp";
    }

    // ====================================================== ACTIVATE (OTP)
    @GetMapping("/verify-activation-otp")
    public String showActivationOtp(HttpSession session, Model model) {
        String email = (String) session.getAttribute("activationEmail");
        if (email == null) return "redirect:/register";
        model.addAttribute("email", email);
        model.addAttribute("resendCooldownSeconds", resendCooldownSeconds(email, com.cinema.model.OtpToken.PURPOSE_ACTIVATION));
        // Pass fallback OTP if email delivery failed
        String fallbackOtp = (String) session.getAttribute("fallbackOtp");
        if (fallbackOtp != null) {
            model.addAttribute("fallbackOtp", fallbackOtp);
            session.removeAttribute("fallbackOtp");
        }
        if (!model.containsAttribute("activationForm"))
            model.addAttribute("activationForm", new OtpForm());
        return "auth/verify-activation-otp";
    }

    @PostMapping("/verify-activation-otp")
    public String doActivationOtp(@Valid @ModelAttribute("activationForm") OtpForm form,
                                  BindingResult binding, HttpSession session, Model model) {
        String email = (String) session.getAttribute("activationEmail");
        if (email == null) return "redirect:/register";
        if (binding.hasErrors()) {
            model.addAttribute("otpError", binding.getFieldError("otp").getDefaultMessage());
            model.addAttribute("email", email);
            return "auth/verify-activation-otp";
        }

        AuthService.OtpResult r = authService.activate(email, form.getOtp());
        switch (r) {
            case SUCCESS -> {
                // Show success message on OTP page before redirecting (so user actually sees it)
                model.addAttribute("activationSuccess", "✅ Tài khoản đã được kích hoạt thành công! Đang chuyển hướng đến trang đăng nhập...");
                model.addAttribute("email", email);
                return "auth/verify-activation-otp";
            }
            case TOO_MANY_ATTEMPTS -> {
                model.addAttribute("formError", "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới.");
                model.addAttribute("email", email);
                return "auth/verify-activation-otp";
            }
            case NO_PENDING_TOKEN -> {
                session.removeAttribute("activationEmail");
                model.addAttribute("formError", "Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng đăng ký lại.");
                return "auth/verify-activation-otp";
            }
            case INVALID_OR_EXPIRED -> {
                model.addAttribute("otpError", "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại.");
                model.addAttribute("email", email);
                // Mark that an error just occurred — prevents auto-submit race
                session.setAttribute("otpErrorJustShown", true);
                return "auth/verify-activation-otp";
            }
        }
        return "redirect:/verify-activation-otp";
    }

    @PostMapping("/verify-activation-otp/resend")
    public String resendActivationOtp(HttpSession session, Model model) {
        String email = (String) session.getAttribute("activationEmail");
        if (email == null) return "redirect:/register";
        AuthService.ResendResult r = authService.resendOtp(email, com.cinema.model.OtpToken.PURPOSE_ACTIVATION);
        if (r.sent()) {
            model.addAttribute("formSuccess", "Đã gửi lại mã OTP. Vui lòng kiểm tra email.");
        } else if ("COOLDOWN".equals(r.code())) {
            model.addAttribute("formError", "Vui lòng chờ " + r.retryAfterSeconds() + " giây trước khi gửi lại.");
        } else {
            // Email failed — show raw OTP.
            model.addAttribute("formError", "Gửi email thất bại. Mã OTP của bạn:");
            model.addAttribute("fallbackOtp", r.rawOtp());
        }
        model.addAttribute("email", email);
        model.addAttribute("resendCooldownSeconds", resendCooldownSeconds(email, com.cinema.model.OtpToken.PURPOSE_ACTIVATION));
        return "auth/verify-activation-otp";
    }

    // ================================================================ LOGIN
    @GetMapping("/login")
    public String showLogin(HttpSession session, Model model,
                            @RequestParam(name = "logout",     required = false) String logout,
                            @RequestParam(name = "activated",  required = false) String activated,
                            @RequestParam(name = "passwordResetSuccess", required = false) String resetOk,
                            @RequestParam(name = "oauthError", required = false) String oauthError,
                            @RequestParam(name = "oauthRejected", required = false) String oauthRejected,
                            @RequestParam(name = "oauthUnavailable", required = false) String oauthUnavailable,
                            @RequestParam(name = "redirect",   required = false) String redirect) {
        if (sessionService.isLoggedIn(session))
            return sessionService.dashboardForRole(sessionService.currentRole(session));
        if (!model.containsAttribute("loginForm"))
            model.addAttribute("loginForm", new LoginForm());
        if (redirect != null) model.addAttribute("redirect", redirect);
        if ("true".equals(logout)) model.addAttribute("logoutSuccess", "Bạn đã đăng xuất thành công.");
        if ("true".equals(activated)) model.addAttribute("activationSuccess", "Tài khoản đã kích hoạt thành công! Vui lòng đăng nhập.");
        if ("true".equals(resetOk)) model.addAttribute("resetSuccess", "Mật khẩu đã được đặt lại. Vui lòng đăng nhập.");
        if (oauthError != null) model.addAttribute("formError", "Đăng nhập Google thất bại: " + oauthError);
        if (oauthRejected != null) model.addAttribute("formError", "Bạn đã từ chối đăng nhập Google.");
        if (oauthUnavailable != null) model.addAttribute("formError", "Đăng nhập Google chưa được cấu hình.");
        return "auth/login";
    }

    /**
     * Spring Security handles this path when Google OAuth is enabled. When it
     * is disabled, return the visitor to login instead of exposing a 404 page.
     */
    @GetMapping("/oauth2/authorization/google")
    public String googleOAuthUnavailable() {
        return "redirect:/login?oauthUnavailable";
    }

    @PostMapping("/login")
    public String doLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                          BindingResult binding, Model model, HttpServletRequest request,
                          @RequestParam(name = "redirect", required = false) String redirect) {
        if (binding.hasErrors()) {
            binding.getFieldErrors().forEach(e ->
                    model.addAttribute(e.getField() + "Error", e.getDefaultMessage()));
            return "auth/login";
        }

        String ip = SessionService.resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        AuthService.LoginResult result =
                authService.login(form.getIdentifier(), form.getPassword(), ip, ua);
        switch (result.status()) {
            case LOCKED_OUT -> {
                int sec = result.retryAfterSeconds();
                String msg = sec > 60
                        ? "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + (sec / 60) + " phút " + (sec % 60) + " giây."
                        : "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + sec + " giây.";
                model.addAttribute("formError", msg);
                model.addAttribute("lockedSeconds", sec);
                return "auth/login";
            }
            case TOO_MANY_IP_FAILURES -> {
                int sec = result.retryAfterSeconds();
                String msg = "Đã có quá nhiều yêu cầu đăng nhập từ thiết bị này. Vui lòng thử lại sau "
                        + (sec / 60) + " phút " + (sec % 60) + " giây.";
                model.addAttribute("formError", msg);
                model.addAttribute("lockedSeconds", sec);
                return "auth/login";
            }
            case EMAIL_NOT_FOUND, WRONG_PASSWORD -> {
                model.addAttribute("formError", "Email hoặc mật khẩu không chính xác.");
                return "auth/login";
            }
            case OAUTH_PASSWORD_NOT_SET -> {
                model.addAttribute("formError", "Tài khoản này được tạo qua Google. Vui lòng đăng nhập bằng nút Google hoặc đặt mật khẩu mới.");
                return "auth/login";
            }
            case INACTIVE_ACCOUNT -> {
                if (result.account() != null
                        && "PENDING_VERIFICATION".equalsIgnoreCase(result.account().getStatus())) {
                    HttpSession activationSession = request.getSession(true);
                    activationSession.setAttribute("activationEmail",
                            result.account().getEmail().trim().toLowerCase());
                    return "redirect:/verify-activation-otp";
                }
                model.addAttribute("formError", "This account is inactive or locked.");
                return "auth/login";
            }
            case UNSUPPORTED_ROLE -> { model.addAttribute("formError", "Vai trò tài khoản chưa được hỗ trợ."); return "auth/login"; }
            case SUCCESS -> { /* continue */ }
        }

        Account account = result.account();
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession fresh = request.getSession(true);
        sessionService.login(fresh, account, SessionService.AUTH_METHOD_PASSWORD);
        return sessionService.resolvePostLoginRedirect(fresh, redirect);
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
            model.addAttribute("forgotForm", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String doForgot(@Valid @ModelAttribute("forgotForm") ForgotPasswordForm form,
                           BindingResult binding, Model model, HttpSession session) {
        if (binding.hasErrors()) {
            binding.getFieldErrors().forEach(e ->
                    model.addAttribute(e.getField() + "Error", e.getDefaultMessage()));
            return "auth/forgot-password";
        }
        boolean ok = authService.requestPasswordReset(form.getEmail());
        if (!ok) {
            // For privacy, even on failure we show a generic message.
            model.addAttribute("formError", "Nếu email phù hợp với một tài khoản, hướng dẫn xác minh sẽ được gửi.");
            return "auth/forgot-password";
        }
        // Always show the same generic success message – never reveal account existence.
        session.setAttribute("resetEmail", form.getEmail().trim().toLowerCase());
        return "redirect:/verify-reset-otp";
    }

    // ====================================================== RESET PASSWORD (OTP)
    @GetMapping("/verify-reset-otp")
    public String showVerifyReset(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot-password";
        model.addAttribute("email", maskEmail(email));
        model.addAttribute("resendCooldownSeconds", resendCooldownSeconds(email, com.cinema.model.OtpToken.PURPOSE_RESET_PASSWORD));
        if (!model.containsAttribute("resetOtpForm"))
            model.addAttribute("resetOtpForm", new OtpForm());
        return "auth/verify-reset-otp";
    }

    @PostMapping("/verify-reset-otp")
    public String doVerifyReset(@Valid @ModelAttribute("resetOtpForm") OtpForm form,
                                BindingResult binding, HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot-password";
        if (binding.hasErrors()) {
            model.addAttribute("otpError", binding.getFieldError("otp").getDefaultMessage());
            model.addAttribute("email", maskEmail(email));
            return "auth/verify-reset-otp";
        }

        AuthService.OtpResult r = authService.preCheckOtp(email, form.getOtp() == null ? "" : form.getOtp().trim(),
                com.cinema.model.OtpToken.PURPOSE_RESET_PASSWORD);
        switch (r) {
            case SUCCESS -> {
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

    // ========================================== API: AJAX verify reset OTP
    @PostMapping("/api/auth/verify-reset-otp")
    public ResponseEntity<?> apiVerifyResetOtp(@Valid @RequestBody OtpForm form, HttpSession session) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phiên hết hạn. Vui lòng yêu cầu mã mới."));
        }
        String otp = form.getOtp().trim();

        AuthService.OtpResult r = authService.preCheckOtp(email, otp, com.cinema.model.OtpToken.PURPOSE_RESET_PASSWORD);
        switch (r) {
            case SUCCESS -> {
                session.setAttribute("resetOtpPending", otp);
                return ResponseEntity.ok(Map.of("success", true, "message", "Xác minh thành công!"));
            }
            case TOO_MANY_ATTEMPTS -> {
                clearResetSession(session);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới."));
            }
            case NO_PENDING_TOKEN -> {
                clearResetSession(session);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới."));
            }
            case INVALID_OR_EXPIRED -> {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã OTP không đúng hoặc đã hết hạn."));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Xác minh thất bại."));
    }

    @PostMapping("/verify-reset-otp/resend")
    public String resendResetOtp(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/forgot-password";
        AuthService.ResendResult r = authService.resendOtp(email, com.cinema.model.OtpToken.PURPOSE_RESET_PASSWORD);
        if (r.sent()) {
            model.addAttribute("formSuccess", "Đã gửi lại mã OTP. Vui lòng kiểm tra email.");
        } else if ("COOLDOWN".equals(r.code())) {
            model.addAttribute("formError", "Vui lòng chờ " + r.retryAfterSeconds() + " giây trước khi gửi lại.");
        } else {
            model.addAttribute("formError", "Không thể gửi lại mã lúc này. Vui lòng thử lại sau.");
        }
        model.addAttribute("email", maskEmail(email));
        model.addAttribute("resendCooldownSeconds", resendCooldownSeconds(email, com.cinema.model.OtpToken.PURPOSE_RESET_PASSWORD));
        return "auth/verify-reset-otp";
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

    // ================================================================ helpers

    private int resendCooldownSeconds(String email, String purpose) {
        return otpService.getResendCooldownSeconds(); // server value; the UI may show it
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
}
