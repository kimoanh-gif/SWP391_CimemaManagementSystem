package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One News row on the Manager News Management screen (M07 / UC35).
 */
public class ManagerNewsItem {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private int newsId;
    private String title;
    private String content;
    private String thumbnailUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private String rejectionReason;
    private String authorName;
    private String authorPosition;
    private String reviewerName;

    public int getNewsId() { return newsId; }
    public void setNewsId(int newsId) { this.newsId = newsId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getStatus() { return status != null ? status.toUpperCase() : ""; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getAuthorName() { return authorName != null ? authorName : "Unknown staff"; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorPosition() { return authorPosition != null ? authorPosition : "Staff"; }
    public void setAuthorPosition(String authorPosition) { this.authorPosition = authorPosition; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    // ------------------------------------------------------------------ display helpers

    /** "#NEWS-088" */
    public String getCode() {
        return String.format("#NEWS-%03d", newsId);
    }

    /**
     * Screen status: PENDING, APPROVED (published in the future = scheduled),
     * PUBLISHED (live), REJECTED, DRAFT, ARCHIVED.
     */
    public String getDisplayStatus() {
        String s = getStatus();
        if ("PUBLISHED".equals(s) && publishedAt != null && publishedAt.isAfter(LocalDateTime.now())) {
            return "APPROVED";
        }
        return s;
    }

    public String getDisplayStatusLabel() {
        return switch (getDisplayStatus()) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "PUBLISHED" -> "Publishing";
            case "REJECTED" -> "Rejected";
            case "DRAFT" -> "Draft";
            case "ARCHIVED" -> "Archived";
            default -> getStatus();
        };
    }

    public boolean isPending() { return "PENDING".equals(getDisplayStatus()); }
    public boolean isScheduled() { return "APPROVED".equals(getDisplayStatus()); }
    public boolean isLive() { return "PUBLISHED".equals(getDisplayStatus()); }
    public boolean isRejected() { return "REJECTED".equals(getDisplayStatus()); }

    /** Two-letter avatar such as "TD". */
    public String getAuthorInitials() {
        String name = getAuthorName().trim();
        if (name.isEmpty()) return "?";
        String[] parts = name.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public String getCreatedDisplay() {
        return createdAt != null ? createdAt.format(DATE_TIME) : "";
    }

    /** Second line under the time: "Updated: 10:45", "Published: 11/10 15:30" ... */
    public String getTimelineDisplay() {
        return switch (getDisplayStatus()) {
            case "PUBLISHED" -> publishedAt != null ? "Published: " + publishedAt.format(SHORT) : "";
            case "APPROVED" -> publishedAt != null ? "Scheduled: " + publishedAt.format(SHORT) : "";
            case "REJECTED", "ARCHIVED" -> reviewedAt != null
                    ? (isRejected() ? "Rejected: " : "Archived: ") + reviewedAt.format(SHORT) : "";
            case "PENDING" -> submittedAt != null ? "Submitted: " + submittedAt.format(SHORT) : "";
            default -> updatedAt != null ? "Updated: " + updatedAt.format(SHORT) : "";
        };
    }

    public String getExcerpt() {
        if (content == null) return "";
        String plain = content.replaceAll("<[^>]*>", "").trim();
        return plain.length() <= 160 ? plain : plain.substring(0, 160) + "...";
    }
}
