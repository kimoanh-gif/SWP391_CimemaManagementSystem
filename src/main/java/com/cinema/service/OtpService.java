package com.cinema.service;

import com.cinema.dao.OtpTokenDAO;
import com.cinema.model.OtpToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates, hashes and verifies 6-digit OTP codes and dispatches them by email.
 * Codes are never stored in plain text – only BCrypt hashes live in dbo.OtpToken.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private static final int    BCRYPT_STRENGTH = 10;
    private static final int    CODE_LENGTH     = 6;

    private final JavaMailSender mailSender;
    private final OtpTokenDAO otpTokenDAO;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    private final SecureRandom random = new SecureRandom();

    @Value("${app.auth.otp.ttl-seconds:300}")
    private int otpTtlSeconds;

    /** Resend cooldown in seconds. */
    @Value("${app.auth.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    /** Maximum OTP codes that can be sent per email per day. */
    @Value("${app.auth.otp.max-send-daily:5}")
    private int maxSendDaily;

    /** Brand / display name shown in the email. */
    @Value("${app.auth.brand:CinemaSystem}")
    private String brand;

    /** From address. Falls back to the spring.mail.username if blank. */
    @Value("${app.auth.mail.from:}")
    private String fromAddress;

    public OtpService(JavaMailSender mailSender, OtpTokenDAO otpTokenDAO) {
        this.mailSender = mailSender;
        this.otpTokenDAO = otpTokenDAO;
    }

    public int getOtpTtlSeconds() { return otpTtlSeconds; }
    public int getResendCooldownSeconds() { return resendCooldownSeconds; }
    public int getMaxAttemptsDefault() { return 5; }
    public int getMaxSendDaily() { return maxSendDaily; }

    /**
     * Check if OTP can be sent to this email today.
     * Returns null if OK, or an error message if limit exceeded.
     */
    public String canSendOtp(String email, String purpose) {
        int todayCount = otpTokenDAO.getTodaySendCount(email, purpose);
        if (todayCount >= maxSendDaily) {
            return "You have reached the daily limit of " + maxSendDaily + " verification codes. Please try again tomorrow.";
        }
        return null; // OK to send
    }

    /**
     * Get remaining OTP sends for today.
     */
    public int getRemainingSends(String email, String purpose) {
        int todayCount = otpTokenDAO.getTodaySendCount(email, purpose);
        return Math.max(0, maxSendDaily - todayCount);
    }

    /** Generates a 6-digit numeric code as a zero-padded String. */
    public String generateCode() {
        return String.format(Locale.ROOT, "%0" + CODE_LENGTH + "d", random.nextInt((int) Math.pow(10, CODE_LENGTH)));
    }

    public String hash(String raw) {
        if (raw == null) return null;
        return encoder.encode(raw);
    }

    public boolean verify(String raw, String hashed) {
        if (raw == null || hashed == null) return false;
        try {
            return encoder.matches(raw.trim(), hashed);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Returns true if a new code may be sent now (no resend cooldown in effect). */
    public boolean canResend(LocalDateTime lastSentAt) {
        if (lastSentAt == null) return true;
        return LocalDateTime.now().isAfter(lastSentAt.plusSeconds(resendCooldownSeconds));
    }

    public int secondsUntilNextResend(LocalDateTime lastSentAt) {
        if (lastSentAt == null) return 0;
        LocalDateTime allowed = lastSentAt.plusSeconds(resendCooldownSeconds);
        long diff = java.time.Duration.between(LocalDateTime.now(), allowed).getSeconds();
        return (int) Math.max(0, diff);
    }

    /**
     * Sends the OTP via SMTP. Returns true on success.
     * <p>Never logs the raw code.
     */
    public boolean sendByEmail(String to, String code, String purpose) {
        if (to == null || to.isBlank() || code == null) return false;
        int minutes = Math.max(1, otpTtlSeconds / 60);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        String subject;
        String body;
        if (OtpToken.PURPOSE_ACTIVATION.equals(purpose)) {
            subject = "[" + brand + "] Account activation code";
            body = """
                    Hello,

                    Your verification code to activate your %s account is:

                        %s

                    This code is valid for %d minutes (until %s).
                    If you did not create this account, you can ignore this email.

                    Regards,
                    The %s team
                    """.formatted(brand, code, minutes, LocalDateTime.now().plusSeconds(otpTtlSeconds).format(fmt), brand);
        } else {
            subject = "[" + brand + "] Password change verification code";
            body = """
                    Hello,

                    Your verification code to change your %s password is:

                        %s

                    This code is valid for %d minutes (until %s).
                    Do not share this code with anyone.

                    If you did not request this change, you can ignore this email.

                    Regards,
                    The %s team
                    """.formatted(brand, code, minutes, LocalDateTime.now().plusSeconds(otpTtlSeconds).format(fmt), brand);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            // Log the delivery failure, but never include the OTP or mail credentials.
            log.error("OTP email delivery failed (purpose={}, recipient={})", purpose, maskEmail(to), ex);
            return false;
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "[invalid]";
        String local = email.substring(0, at);
        String visible = local.length() == 1 ? local : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }
}
