package com.cinema.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Screen status of a Promotion or Voucher (M11).
 * Stored statuses: DRAFT, ACTIVE, PAUSED, INACTIVE (stopped permanently), EXPIRED.
 * Shown statuses add UPCOMING (active, not started yet) and EXPIRED (end date passed).
 */
public final class PromotionLifecycle {

    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String INACTIVE = "INACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String UPCOMING = "UPCOMING";

    private PromotionLifecycle() {
    }

    public static String displayStatus(String stored, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        String s = stored == null ? "" : stored.trim().toUpperCase();
        switch (s) {
            case DRAFT, PAUSED, INACTIVE:
                return s;
            case EXPIRED:
                return EXPIRED;
            default:
                if (end != null && end.isBefore(now)) return EXPIRED;
                if (start != null && start.isAfter(now)) return UPCOMING;
                return ACTIVE;
        }
    }

    public static String label(String displayStatus) {
        return switch (displayStatus) {
            case ACTIVE -> "Active";
            case UPCOMING -> "Upcoming";
            case PAUSED -> "Paused";
            case DRAFT -> "Draft";
            case INACTIVE -> "Stopped";
            case EXPIRED -> "Expired";
            default -> displayStatus;
        };
    }

    /** "290 days left", "Starts in 3 days", "Ended" ... */
    public static String timeHint(String displayStatus, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (EXPIRED.equals(displayStatus) || (end != null && end.isBefore(now))) return "Ended";
        if (UPCOMING.equals(displayStatus) && start != null) {
            long days = Math.max(0, Duration.between(now, start).toDays());
            return days == 0 ? "Starts today" : "Starts in " + days + " days";
        }
        if (PAUSED.equals(displayStatus)) return "Paused by manager";
        if (DRAFT.equals(displayStatus)) return "Not started";
        if (INACTIVE.equals(displayStatus)) return "Stopped permanently";
        if (end == null) return "";
        long days = Math.max(0, Duration.between(now, end).toDays());
        return days == 0 ? "Ends today" : days + " days left";
    }
}
