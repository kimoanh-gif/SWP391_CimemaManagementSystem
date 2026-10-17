package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a News/Article for Screen 21 (Home) and 25 (Promotions & News).
 */
public class NewsArticle {

    private Integer newsId;
    private Integer createdByStaffId;
    private Integer reviewedByAccountId;
    private String title;
    private String content;
    private String summary;
    private String thumbnailUrl;
    private String status; // PUBLISHED, DRAFT, PENDING, REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String authorName;
    private String category = "Điện Ảnh";

    public NewsArticle() {
    }

    public Integer getNewsId() {
        return newsId;
    }

    public void setNewsId(Integer newsId) {
        this.newsId = newsId;
    }

    public Integer getCreatedByStaffId() {
        return createdByStaffId;
    }

    public void setCreatedByStaffId(Integer createdByStaffId) {
        this.createdByStaffId = createdByStaffId;
    }

    public Integer getReviewedByAccountId() {
        return reviewedByAccountId;
    }

    public void setReviewedByAccountId(Integer reviewedByAccountId) {
        this.reviewedByAccountId = reviewedByAccountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        return getExcerpt(140);
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getThumbnailUrl() {
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            return thumbnailUrl;
        }
        return "/images/cinema-login-hero.png";
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getStatus() {
        return status != null ? status : "PUBLISHED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt != null ? publishedAt : createdAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getAuthorName() {
        return authorName != null ? authorName : "CineVerse Team";
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCategory() {
        return category != null ? category : "Tin Điện Ảnh";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Helper methods
    public String getFormattedPublishedDate() {
        LocalDateTime dt = getPublishedAt();
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getExcerpt(int maxChars) {
        if (content == null || content.isBlank()) return "";
        // Strip HTML if any
        String plain = content.replaceAll("<[^>]*>", "").trim();
        if (plain.length() <= maxChars) return plain;
        return plain.substring(0, maxChars) + "...";
    }
}
