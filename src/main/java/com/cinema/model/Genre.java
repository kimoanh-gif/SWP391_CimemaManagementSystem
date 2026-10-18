package com.cinema.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified Genre entity representing Movie Genres for Customer catalog and Manager module.
 */
public class Genre {

    private Integer genreId;
    private String name;
    private String description;
    private String status; // 'ACTIVE' or 'INACTIVE'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int movieCount;
    private List<String> associatedMovieTitles = new ArrayList<>();

    public Genre() {
    }

    public Genre(Integer genreId, String name) {
        this.genreId = genreId;
        this.name = name;
    }

    public Genre(Integer genreId, String name, String description, String status) {
        this.genreId = genreId;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getMovieCount() {
        return movieCount;
    }

    public void setMovieCount(int movieCount) {
        this.movieCount = movieCount;
    }

    public List<String> getAssociatedMovieTitles() {
        return associatedMovieTitles;
    }

    public void setAssociatedMovieTitles(List<String> associatedMovieTitles) {
        this.associatedMovieTitles = associatedMovieTitles != null ? associatedMovieTitles : new ArrayList<>();
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
}
