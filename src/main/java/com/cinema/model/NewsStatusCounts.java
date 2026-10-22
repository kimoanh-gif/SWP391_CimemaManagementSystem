package com.cinema.model;

import java.util.Locale;
import java.util.Map;

/** Number of news articles per review status, for the M07 metric cards and filter. */
public class NewsStatusCounts {

    private final Map<String, Integer> counts;
    private final int submittedToday;

    public NewsStatusCounts(Map<String, Integer> counts, int submittedToday) {
        this.counts = counts == null ? Map.of() : counts;
        this.submittedToday = submittedToday;
    }

    public int get(String status) {
        return status == null ? 0 : counts.getOrDefault(status.toUpperCase(Locale.ROOT), 0);
    }

    public int getTotal() { return counts.values().stream().mapToInt(Integer::intValue).sum(); }
    public int getPending() { return get("PENDING"); }
    public int getApproved() { return get("APPROVED"); }
    public int getRejected() { return get("REJECTED"); }
    public int getPublished() { return get("PUBLISHED"); }
    public int getDraft() { return get("DRAFT"); }
    public int getSubmittedToday() { return submittedToday; }
}
