package com.cinema.model;

/**
 * KPI cards of the Manager News Management screen.
 * approved = PUBLISHED with a future PublishedAt (scheduled); publishing = live on App/Web.
 * urgentPending = pending for more than 24 hours.
 */
public record ManagerNewsStats(int total, int pending, int urgentPending, int approved,
                               int rejected, int publishing, int drafts, int archived) {

    public static ManagerNewsStats empty() {
        return new ManagerNewsStats(0, 0, 0, 0, 0, 0, 0, 0);
    }
}
