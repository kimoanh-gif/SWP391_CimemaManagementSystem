package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * One news article as seen by the Manager on the News Management screen (M07).
 * Unlike {@link NewsArticle}, getters return the raw database values (no public-page fallbacks).
 */
public class ManagerNewsItem {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private int newsId;
    private String title;
    private String content;
    private String thumbnailUrl;
    private String status;
    private String authorName;
    private String authorPosition;
    private String reviewerName;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;

    public int getNewsId() { return newsId; }
    public void setNewsId(int newsId) { this.newsId = newsId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getStatus() { return status == null ? "" : status.toUpperCase(Locale.ROOT); }
    public void setStatus(String status) { this.status = status; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorPosition() { return authorPosition; }
    public void setAuthorPosition(String authorPosition) { this.authorPosition = authorPosition; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
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

    // ---------------------------------------------------------------- view helpers

    public String getCode() {
        return String.format("#NEWS-%03d", newsId);
    }

    public boolean isPending()   { return "PENDING".equals(getStatus()); }
    public boolean isApproved()  { return "APPROVED".equals(getStatus()); }
    public boolean isRejected()  { return "REJECTED".equals(getStatus()); }
    public boolean isPublished() { return "PUBLISHED".equals(getStatus()); }
    public boolean isDraft()     { return "DRAFT".equals(getStatus()); }

    public String getStatusLabel() {
        return switch (getStatus()) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "PUBLISHED" -> "Published";
            case "DRAFT" -> "Draft";
            default -> status == null ? "Unknown" : status;
        };
    }

    /** One line under the creation time describing the latest workflow event. */
    public String getActivityLabel() {
        return switch (getStatus()) {
            case "PUBLISHED" -> publishedAt == null ? "Published" : "Published: " + publishedAt.format(SHORT);
            case "APPROVED" -> reviewedAt == null ? "Approved" : "Approved: " + reviewedAt.format(SHORT);
            case "REJECTED" -> reviewedAt == null ? "Rejected" : "Rejected: " + reviewedAt.format(SHORT);
            case "PENDING" -> submittedAt == null ? "Awaiting review" : "Submitted: " + submittedAt.format(SHORT);
            default -> updatedAt == null ? "Not submitted" : "Updated: " + updatedAt.format(TIME);
        };
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DATE_TIME);
    }

    public String getAuthorInitials() {
        if (authorName == null || authorName.isBlank()) return "?";
        String[] parts = authorName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    public boolean isSubmittedToday() {
        return submittedAt != null && submittedAt.toLocalDate().equals(LocalDate.now());
    }

    public boolean hasThumbnail() {
        return thumbnailUrl != null && !thumbnailUrl.isBlank();
    }
}
