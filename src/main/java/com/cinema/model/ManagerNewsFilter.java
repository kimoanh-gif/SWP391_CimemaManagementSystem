package com.cinema.model;

import java.util.List;
import java.util.Locale;

/**
 * Search / filter / paging values of the Manager News Management screen.
 * Invalid values fall back to safe defaults.
 */
public record ManagerNewsFilter(String keyword, String status, String period, int page, int size) {

    public static final List<String> STATUSES = List.of("ALL", "PENDING", "APPROVED", "PUBLISHED", "REJECTED", "DRAFT", "ARCHIVED");
    public static final List<String> PERIODS = List.of("ALL", "WEEK", "30DAYS", "MONTH");
    public static final List<Integer> PAGE_SIZES = List.of(5, 10, 20, 50);

    public static ManagerNewsFilter of(String keyword, String status, String period, Integer page, Integer size) {
        String k = keyword == null ? "" : keyword.trim();
        if (k.length() > 100) k = k.substring(0, 100);
        String s = status == null ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(s)) s = "ALL";
        String p = period == null ? "ALL" : period.trim().toUpperCase(Locale.ROOT);
        if (!PERIODS.contains(p)) p = "ALL";
        int z = size != null && PAGE_SIZES.contains(size) ? size : 10;
        int pg = page != null && page > 0 ? page : 1;
        return new ManagerNewsFilter(k, s, p, pg, z);
    }

    public int offset() {
        return (page - 1) * size;
    }
}
