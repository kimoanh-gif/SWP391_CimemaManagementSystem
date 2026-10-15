package com.cinema.service;

import com.cinema.model.OtpToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private static final int    BCRYPT_STRENGTH = 10;
    private static final int    CODE_LENGTH     = 6;

    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    private final SecureRandom random = new SecureRandom();

    @Value("${app.auth.otp.ttl-seconds:300}")
    private int otpTtlSeconds;

    /** Resend cooldown in seconds. */
    @Value("${app.auth.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    /** Brand / display name shown in the email. */
    @Value("${app.auth.brand:CinemaSystem}")
    private String brand;

    /** From address. Falls back to the spring.mail.username if blank. */
    @Value("${app.auth.mail.from:}")
    private String fromAddress;

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public int getOtpTtlSeconds() { return otpTtlSeconds; }
    public int getResendCooldownSeconds() { return resendCooldownSeconds; }
    public int getMaxAttemptsDefault() { return 5; }

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
            subject = "[" + brand + "] Mã xác minh kích hoạt tài khoản";
            body = """
                    Xin chào,

                    Mã OTP để kích hoạt tài khoản %s của bạn là:

                        %s

                    Mã có hiệu lực trong %d phút (đến %s).
                    Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.

                    Trân trọng,
                    Đội ngũ %s
                    """.formatted(brand, code, minutes, LocalDateTime.now().plusSeconds(otpTtlSeconds).format(fmt), brand);
        } else {
            subject = "[" + brand + "] Mã xác minh đặt lại mật khẩu";
            body = """
                    Xin chào,

                    Mã OTP để đặt lại mật khẩu %s của bạn là:

                        %s

                    Mã có hiệu lực trong %d phút (đến %s).
                    Không chia sẻ mã này với bất kỳ ai.

                    Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.

                    Trân trọng,
                    Đội ngũ %s
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
            // Email failed – return false so caller can surface the raw OTP code to the user.
            return false;
        }
    }
}
