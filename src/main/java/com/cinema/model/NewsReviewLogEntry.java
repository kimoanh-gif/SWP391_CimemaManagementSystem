package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** One row of the news review history drawer (dbo.NewsReviewLog). */
public class NewsReviewLogEntry {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm");

    private int newsId;
    private String newsTitle;
    private String reviewerName;
    private String action;
    private String note;
    private LocalDateTime createdAt;

    public int getNewsId() { return newsId; }
    public void setNewsId(int newsId) { this.newsId = newsId; }
    public String getNewsTitle() { return newsTitle; }
    public void setNewsTitle(String newsTitle) { this.newsTitle = newsTitle; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getAction() { return action == null ? "" : action.toUpperCase(Locale.ROOT); }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCode() { return String.format("#NEWS-%03d", newsId); }

    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(FORMAT); }

    public String getActionVerb() {
        return switch (getAction()) {
            case "APPROVE" -> "approved";
            case "PUBLISH" -> "published";
            case "REJECT" -> "rejected";
            case "UNPUBLISH" -> "unpublished";
            case "REOPEN" -> "reopened for review";
            default -> getAction().toLowerCase(Locale.ROOT);
        };
    }

    /** Tailwind border colour class for the timeline item. */
    public String getToneClass() {
        return switch (getAction()) {
            case "REJECT" -> "border-[#C2202E]";
            case "UNPUBLISH", "REOPEN" -> "border-cinema-gold";
            case "PUBLISH" -> "border-[#5C388E]";
            default -> "border-[#0E7044]";
        };
    }
}
