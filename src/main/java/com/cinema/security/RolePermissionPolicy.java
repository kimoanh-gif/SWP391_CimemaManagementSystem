package com.cinema.security;

import java.util.*;

/** Business boundaries; granting a permission never changes an account's role. */
public final class RolePermissionPolicy {
    private RolePermissionPolicy() {}

    public static final Map<String, Set<String>> ALLOWED = Map.of(
            "CUSTOMER", Set.of("movie:read", "booking:create"),
            "STAFF", Set.of("movie:read", "booking:create", "ticket:checkin", "booking:read", "news:manage"),
            "MANAGER", Set.of("movie:read", "showtime:manage", "report:view", "booking:read",
                    "movie:manage", "genre:manage", "room:manage", "seat:manage", "pricing:manage",
                    "fb:manage", "voucher:manage", "promotion:manage", "news:approve"),
            "ADMIN", Set.of("movie:read", "booking:create", "ticket:checkin",
                    "showtime:manage", "report:view", "account:manage", "booking:read", "news:manage", "news:approve",
                    "movie:manage", "genre:manage", "room:manage", "seat:manage", "pricing:manage",
                    "fb:manage", "voucher:manage", "promotion:manage"));
    /** New permissions seeded once; later revocations are preserved across restarts. */
    public static final Map<String, String> ADDITIONAL = Map.ofEntries(
            Map.entry("booking:read", "Xem danh sách và chi tiết đặt vé"),
            Map.entry("news:manage", "Soạn và quản lý bài viết"),
            Map.entry("news:approve", "Duyệt và xuất bản bài viết"),
            Map.entry("movie:manage", "Quản lý phim"),
            Map.entry("genre:manage", "Quản lý thể loại phim"),
            Map.entry("room:manage", "Quản lý phòng chiếu"),
            Map.entry("seat:manage", "Quản lý loại ghế và sơ đồ ghế"),
            Map.entry("pricing:manage", "Quản lý giá vé"),
            Map.entry("fb:manage", "Quản lý đồ ăn và thức uống"),
            Map.entry("voucher:manage", "Quản lý voucher"),
            Map.entry("promotion:manage", "Quản lý khuyến mãi"));

    public static final Map<String, Set<String>> REQUIRED = Map.of(
            "CUSTOMER", Set.of("movie:read"),
            "ADMIN", Set.of("movie:read", "account:manage"),
            "MANAGER", Set.of("movie:read", "report:view"),
            "STAFF", Set.of("movie:read", "ticket:checkin"));

    public static Set<String> allowed(String role) {
        return ALLOWED.getOrDefault(normalize(role), Set.of());
    }
    public static Set<String> required(String role) {
        return REQUIRED.getOrDefault(normalize(role), Set.of());
    }
    public static Set<String> effective(String role, Set<String> assigned) {
        Set<String> result = new HashSet<>(assigned);
        result.retainAll(allowed(role));
        return Set.copyOf(result);
    }
    private static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
