package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.model.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Clock;

/** Shared customer/staff reset. Plaintext is sent only to the account's registered email. */
@Service
public class AdminPasswordResetService {
    private final AccountDAO accounts;
    private final JavaMailSender mail;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<Integer,Long> nextResetAt = new ConcurrentHashMap<>();
    private final Clock clock;
    private static final long COOLDOWN_MS = 60_000;
    @Value("${app.auth.mail.from:${spring.mail.username:}}")
    private String from;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminPasswordResetService(AccountDAO accounts, JavaMailSender mail, PasswordEncoder encoder) {
        this(accounts,mail,encoder,Clock.systemUTC());
    }
    AdminPasswordResetService(AccountDAO accounts, JavaMailSender mail, PasswordEncoder encoder, Clock clock) {
        this.accounts=accounts; this.mail=mail; this.encoder=encoder;
        this.clock=clock;
    }
    public record Result(boolean success, String code, String message) {}
    public static class DeliveryFailure extends RuntimeException {
        public DeliveryFailure() { super("Password reset email could not be delivered."); }
    }

    @Transactional
    public Result reset(int accountId) {
        Account account=accounts.findByIdForPasswordReset(accountId).orElse(null);
        if (account==null) return new Result(false,"NOT_FOUND","Account not found.");
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return new Result(false,"INACTIVE_ACCOUNT","Activate or reactivate the account before resetting its password.");
        }
        if (account.getEmail()==null || !com.cinema.util.AccountValidation.EMAIL.matcher(account.getEmail().trim()).matches()) {
            return new Result(false,"NO_EMAIL","Account has no valid registered email address.");
        }
        long now=clock.millis();
        nextResetAt.entrySet().removeIf(entry -> entry.getValue() <= now);
        // Atomic even when two administrators reset the same account concurrently.
        if (nextResetAt.putIfAbsent(accountId,now+COOLDOWN_MS)!=null) {
            return new Result(false,"RESET_COOLDOWN","Please wait 60 seconds between password reset attempts for this account.");
        }
        String password=generatePassword();
        if (accounts.updatePasswordHash(accountId,encoder.encode(password))!=1) {
            throw new org.springframework.dao.DataAccessResourceFailureException("Password update failed.");
        }
        SimpleMailMessage message=new SimpleMailMessage();
        message.setTo(account.getEmail().trim());
        if (from!=null && !from.isBlank()) message.setFrom(from);
        message.setSubject("[CINEORA] Your account password has been reset");
        message.setText("""
                Hello,

                An administrator has reset your CINEORA account password.

                Username: %s
                New password: %s

                Sign in with your registered email or username and the new password above.
                Please change this password after signing in. Do not share it with anyone.
                If you did not request this reset, please contact the cinema administrator.

                CINEORA
                """.formatted(account.getUsername(),password));
        try {
            mail.send(message);
        } catch (MailException ex) {
            // No original mail exception/cause: it can contain the plaintext message.
            // Propagate through the transaction proxy to roll back the password update.
            throw new DeliveryFailure();
        }
        return new Result(true,null,"Password reset successfully. The new password was sent to the registered email. Please change it after signing in.");
    }

    private String generatePassword() {
        String upper="ABCDEFGHJKLMNPQRSTUVWXYZ",lower="abcdefghjkmnpqrstuvwxyz",digits="23456789",special="!@#$%";
        String all=upper+lower+digits+special;
        char[] value=new char[16];
        value[0]=upper.charAt(random.nextInt(upper.length()));
        value[1]=lower.charAt(random.nextInt(lower.length()));
        value[2]=digits.charAt(random.nextInt(digits.length()));
        value[3]=special.charAt(random.nextInt(special.length()));
        for(int i=4;i<value.length;i++) value[i]=all.charAt(random.nextInt(all.length()));
        for(int i=value.length-1;i>0;i--) {int j=random.nextInt(i+1);char c=value[i];value[i]=value[j];value[j]=c;}
        return new String(value);
    }
}
