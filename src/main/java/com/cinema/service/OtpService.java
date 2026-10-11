package com.cinema.service;

import com.cinema.model.OtpToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Generates / hashes / verifies 6-digit OTPs and dispatches them by email.
 * Codes are never stored in plain text – only BCrypt hashes live in dbo.OtpToken.
 *
 * Responsibilities (kept narrow on purpose):
 *   - entropy:        secure-random 6-digit code generation
 *   - hashing:        BCrypt hash / verify (strength 10)
 *   - email delivery: SMTP send
 *
 * Token persistence (insert / delete / mark consumed) is owned by AuthService
 * which already has OtpTokenDAO injected.
 */
@Service
public class OtpService {

    private static final int BCRYPT_STRENGTH = 10;

    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    private final SecureRandom random = new SecureRandom();

    @Value("${app.auth.otp.ttl-seconds:300}")
    private int otpTtlSeconds;

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public int getOtpTtlSeconds() { return otpTtlSeconds; }

    /** Generates a 6-digit numeric code as a zero-padded String. */
    public String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean verify(String raw, String hash) {
        if (raw == null || hash == null) return false;
        try {
            return encoder.matches(raw.trim(), hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean sendByEmail(String to, String code, String purpose) {
        if (to == null || to.isBlank() || code == null) return false;
        int minutes = Math.max(1, otpTtlSeconds / 60);

        String subject;
        String body;
        if (OtpToken.PURPOSE_ACTIVATION.equals(purpose)) {
            subject = "[CinemaSystem] Xac thuc tai khoan cua ban";
            body = """
                    Xin chao,

                    Ma OTP de kich hoat tai khoan CinemaSystem cua ban la: %s
                    Ma co hieu luc trong %d phut.

                    Neu ban khong yeu cau dang ky, hay bo qua email nay.
                    """.formatted(code, minutes);
        } else {
            subject = "[CinemaSystem] Dat lai mat khau";
            body = """
                    Xin chao,

                    Ma OTP de dat lai mat khau CinemaSystem cua ban la: %s
                    Ma co hieu luc trong %d phut. Khong chia se ma nay voi bat ky ai.

                    Neu ban khong yeu cau dat lai mat khau, hay bo qua email nay.
                    """.formatted(code, minutes);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            return false;
        }
    }
}
