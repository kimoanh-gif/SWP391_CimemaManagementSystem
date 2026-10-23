package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a Movie Review for Screen 23 (Movie Details).
 */
public class MovieReview {

    private Integer reviewId;
    private Integer customerId;
    private String customerName;
    private Integer movieId;
    private Integer rating; // 1 to 5
    private String content;
    private String status;
    private LocalDateTime createdAt;

    public MovieReview() {
    }

    public MovieReview(Integer reviewId, Integer customerId, String customerName,
                       Integer movieId, Integer rating, String content, LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.movieId = movieId;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return (customerName != null && !customerName.isBlank()) ? customerName : "CINEORA Guest";
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getRating() {
        return rating != null ? rating : 5;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
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

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getInitial() {
        String name = getCustomerName();
        return (name.length() > 0) ? name.substring(0, 1).toUpperCase() : "U";
    }
}
