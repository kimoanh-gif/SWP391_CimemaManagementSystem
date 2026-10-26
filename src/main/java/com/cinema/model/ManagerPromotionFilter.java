package com.cinema.model;

import java.util.List;
import java.util.Locale;

/**
 * Search / filter / paging of the M11 list. Invalid values fall back to defaults.
 */
public record ManagerPromotionFilter(String keyword, String status, String type, int page, int size) {

    public static final List<String> STATUSES = List.of("ALL", "ACTIVE", "UPCOMING", "PAUSED", "DRAFT", "EXPIRED", "INACTIVE");
    public static final List<String> TYPES = List.of("ALL", "PERCENT", "FIXED");
    public static final List<Integer> PAGE_SIZES = List.of(6, 10, 20, 50);

    public static ManagerPromotionFilter of(String keyword, String status, String type, Integer page, Integer size) {
        String k = keyword == null ? "" : keyword.trim();
        if (k.length() > 100) k = k.substring(0, 100);
        String s = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(s)) s = "ALL";
        String t = type == null ? "ALL" : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(t)) t = "ALL";
        int z = size != null && PAGE_SIZES.contains(size) ? size : 6;
        int p = page != null && page > 0 ? page : 1;
        return new ManagerPromotionFilter(k, s, t, p, z);
    }

    public boolean matchesKeyword(String... values) {
        if (keyword.isEmpty()) return true;
        String k = keyword.toLowerCase(Locale.ROOT);
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains(k)) return true;
        }
        return false;
    }

    public boolean matchesStatus(String displayStatus) {
        return "ALL".equals(status) || status.equals(displayStatus);
    }

    public boolean matchesType(String discountType) {
        if ("ALL".equals(type)) return true;
        boolean percent = VoucherDiscounts.isPercentage(discountType);
        return "PERCENT".equals(type) == percent;
    }
}
