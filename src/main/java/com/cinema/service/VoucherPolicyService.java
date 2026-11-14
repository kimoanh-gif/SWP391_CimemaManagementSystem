package com.cinema.service;

import com.cinema.dao.SystemSettingDAO;
import com.cinema.dto.VoucherForm;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/** Admin limits are checked when saving/activating; issued voucher terms remain stored on the voucher. */
@Service
public class VoucherPolicyService {
    public record Rule(String key, long min, long max, long fallback) {}
    public static final List<Rule> RULES = List.of(
            new Rule("VOUCHER_VALIDITY_DAYS", 1, 365, 30),
            new Rule("VOUCHER_MIN_BOOKING_AMOUNT", 0, 999999999999L, 50000),
            new Rule("VOUCHER_MAX_DISCOUNT_AMOUNT", 0, 999999999999L, 0),
            new Rule("VOUCHER_MAX_DISCOUNT_PERCENT", 1, 100, 50),
            new Rule("VOUCHER_TOTAL_LIMIT", 1, Integer.MAX_VALUE, 1000),
            new Rule("VOUCHER_PER_USER_LIMIT", 1, 100, 5));
    public record Policy(long validityDays, BigDecimal minBookingAmount, BigDecimal maxDiscountAmount,
                         BigDecimal maxPercent, int totalLimit, int perAccountLimit, LocalDateTime updatedAt) {
        public Map<String, String> violations(VoucherForm f) {
            Map<String, String> errors = new LinkedHashMap<>();
            if ("PERCENT".equals(f.getDiscountType())) {
                if (f.getDiscountValue() != null && f.getDiscountValue().compareTo(maxPercent) > 0)
                    errors.put("discountValue", "Admin policy allows at most " + maxPercent + "%.");
                if (maxDiscountAmount.signum() > 0 && (f.getMaxDiscountAmount() == null
                        || f.getMaxDiscountAmount().signum() <= 0 || f.getMaxDiscountAmount().compareTo(maxDiscountAmount) > 0))
                    errors.put("maxDiscountAmount", "Admin policy requires a cap from 1 to " + maxDiscountAmount + " VND.");
            }
            if (minBookingAmount.signum() > 0 && (f.getMinBookingAmount() == null || f.getMinBookingAmount().compareTo(minBookingAmount) < 0))
                errors.put("minBookingAmount", "Admin policy requires a minimum order of at least " + minBookingAmount + " VND.");
            if (f.getValidFrom() != null && f.getValidTo() != null
                    && Duration.between(f.getValidFrom(), f.getValidTo()).compareTo(Duration.ofDays(validityDays)) > 0)
                errors.put("validTo", "Admin policy allows at most " + validityDays + " days from start to end.");
            if (f.getTotalUsageLimit() == null || f.getTotalUsageLimit() > totalLimit)
                errors.put("totalUsageLimit", "Admin policy allows at most " + totalLimit + " uses per voucher; unlimited is not allowed.");
            if (f.getPerAccountLimit() == null || f.getPerAccountLimit() > perAccountLimit)
                errors.put("perAccountLimit", "Admin policy allows at most " + perAccountLimit + " uses per customer per voucher.");
            return errors;
        }
    }
    private final SystemSettingDAO dao;
    public VoucherPolicyService(SystemSettingDAO dao) { this.dao = dao; }
    public static boolean valid(Rule rule, String value) {
        if (value == null || !value.matches("[0-9]{1,12}")) return false;
        long number = Long.parseLong(value);
        return number >= rule.min() && number <= rule.max();
    }
    public Policy current() {
        var rows = dao.findByCategory("VOUCHER");
        Map<String, Long> values = new HashMap<>();
        LocalDateTime updated = null;
        for (var rule : RULES) {
            var setting = rows.stream().filter(s -> rule.key().equals(s.getSettingKey()) && !Boolean.FALSE.equals(s.getIsActive()))
                    .findFirst().orElse(null);
            if (setting != null && !valid(rule, setting.getValue()))
                throw new com.cinema.exception.PromotionAdminException("Invalid Admin voucher policy: " + rule.key());
            values.put(rule.key(), setting == null ? rule.fallback() : Long.parseLong(setting.getValue()));
            if (setting != null && setting.getUpdatedAt() != null && (updated == null || setting.getUpdatedAt().isAfter(updated))) updated = setting.getUpdatedAt();
        }
        return new Policy(values.get("VOUCHER_VALIDITY_DAYS"), BigDecimal.valueOf(values.get("VOUCHER_MIN_BOOKING_AMOUNT")),
                BigDecimal.valueOf(values.get("VOUCHER_MAX_DISCOUNT_AMOUNT")), BigDecimal.valueOf(values.get("VOUCHER_MAX_DISCOUNT_PERCENT")),
                values.get("VOUCHER_TOTAL_LIMIT").intValue(), values.get("VOUCHER_PER_USER_LIMIT").intValue(), updated);
    }
}
