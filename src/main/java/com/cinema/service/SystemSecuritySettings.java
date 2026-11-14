package com.cinema.service;

import com.cinema.dao.SystemSettingDAO;
import com.cinema.model.SystemSetting;
import org.springframework.stereotype.Service;
import java.util.*;

/** Reads persisted security policy without caching, so updates apply to the next request. */
@Service
public class SystemSecuritySettings {
    public record Rule(String key, String name, int min, int max, int defaultValue, String unit) {}
    public static final List<Rule> RULES = List.of(
        new Rule("SYSTEM_BOOKING_REMINDER_MINUTES", "Booking Reminder Time",1,10080,60,"minutes before showtime"),
        new Rule("SYSTEM_PASSWORD_MIN_LENGTH", "Password Minimum Length",8,32,8,"characters"),
        new Rule("SYSTEM_LOGIN_ATTEMPTS", "Max Login Attempts",1,10,5,"attempts"),
        new Rule("SYSTEM_LOGIN_LOCKOUT_MINUTES", "Login Lockout Minutes",1,1440,5,"minutes"),
        new Rule("SYSTEM_OTP_EXPIRY_MINUTES", "OTP Expiry Time",1,30,5,"minutes"),
        new Rule("SYSTEM_OTP_MAX_ATTEMPTS", "OTP Max Attempts",1,10,5,"attempts"),
        new Rule("SYSTEM_OTP_LOCKOUT_MINUTES", "OTP Lockout Minutes",1,1440,15,"minutes"),
        new Rule("SYSTEM_OTP_MAX_EMAIL_REQUESTS_PER_HOUR", "OTP Max Email Requests Per Hour",1,100,3,"requests/email/hour"),
        new Rule("SYSTEM_OTP_MAX_SEND_DAILY", "OTP Max Sends Per Day",1,100,5,"requests/email/day"),
        new Rule("SYSTEM_OTP_RESEND_COOLDOWN", "OTP Resend Cooldown",10,300,60,"seconds"),
        new Rule("SYSTEM_SESSION_TIMEOUT_MINUTES", "Session Timeout",5,480,30,"minutes")
    );
    private final SystemSettingDAO dao;
    public SystemSecuritySettings(SystemSettingDAO dao) { this.dao = dao; }
    private static String normalize(String text) { return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT); }
    public static Rule ruleFor(SystemSetting setting) {
        for (var rule : RULES) {
            if (rule.key().equals(setting.getSettingKey()) || normalize(rule.name()).equals(normalize(setting.getSettingName()))
                    || normalize(rule.key().replace("SYSTEM_", "")).equals(normalize(setting.getSettingKey()))) return rule;
        }
        return null;
    }
    public static boolean valid(Rule rule, String value) {
        if (value == null || !value.matches("[0-9]{1,9}")) return false;
        int parsed = Integer.parseInt(value);
        return parsed >= rule.min() && parsed <= rule.max();
    }
    public int read(String key, int fallback) {
        var rule = RULES.stream().filter(r -> r.key().equals(key)).findFirst().orElseThrow();
        var candidates = dao.findByCategory("SYSTEM").stream()
                .filter(s -> !Boolean.FALSE.equals(s.getIsActive()) && rule.equals(ruleFor(s)))
                .sorted(Comparator.comparing(s -> !key.equals(s.getSettingKey()))).toList();
        if (!candidates.isEmpty() && valid(rule, candidates.get(0).getValue())) return Integer.parseInt(candidates.get(0).getValue());
        return Math.max(rule.min(), Math.min(rule.max(), fallback));
    }
    public static List<SystemSetting> editable(List<SystemSetting> rows) {
        Map<String,SystemSetting> result = new LinkedHashMap<>();
        for (var row : rows) {
            if (Boolean.FALSE.equals(row.getIsActive())) continue;
            var rule = ruleFor(row);
            if (rule == null) continue;
            var previous = result.get(rule.key());
            if (previous != null && !rule.key().equals(row.getSettingKey())) continue;
            row.setValidationRules("min:" + rule.min() + ",max:" + rule.max());
            row.setValueType("NUMBER");
            row.setDescription(rule.unit());
            if (!valid(rule, row.getDefaultValue())) row.setDefaultValue(String.valueOf(rule.defaultValue()));
            result.put(rule.key(),row);
        }
        return new ArrayList<>(result.values());
    }
}
