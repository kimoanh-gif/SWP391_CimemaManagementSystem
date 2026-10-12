package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Movie managed and displayed by the cinema system.
 * Screen 54 - Movie Management (UC-26 / SWP391)
 */
public class Movie {

    private Integer movieId;
    private String title;
    private String originalTitle;
    private Integer duration; // in minutes
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String language;
    private String director;
    private String actors;
    private String ageRating; // 'P', 'K', 'T13', 'T16', 'T18'
    private String description;
    private String poster;
    private String trailer;
    private String status; // 'Upcoming', 'Now Showing', 'Ended', 'Archived'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Genre> genres = new ArrayList<>();
    private int showtimeCount;

    public Movie() {
    }

    public Movie(Integer movieId, String title, Integer duration, String status) {
        this.movieId = movieId;
        this.title = title;
        this.duration = duration;
        this.status = status;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getId() {
        return movieId;
    }

    public void setId(Integer id) {
        this.movieId = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getActors() {
        return actors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
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

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres != null ? genres : new ArrayList<>();
    }

    public List<Integer> getGenreIds() {
        if (genres == null) return new ArrayList<>();
        return genres.stream()
                .map(Genre::getGenreId)
                .collect(Collectors.toList());
    }

    public String getGenreIdsString() {
        if (genres == null || genres.isEmpty()) {
            return "";
        }
        return genres.stream()
                .map(g -> String.valueOf(g.getGenreId()))
                .collect(Collectors.joining(","));
    }

    public String getGenreNamesString() {
        if (genres == null || genres.isEmpty()) {
            return "Chưa phân loại";
        }
        return genres.stream()
                .map(Genre::getName)
                .collect(Collectors.joining(", "));
    }

    public int getShowtimeCount() {
        return showtimeCount;
    }

    public void setShowtimeCount(int showtimeCount) {
        this.showtimeCount = showtimeCount;
    }

    /**
     * BR-07: Only Upcoming and Now Showing movies can be selected when creating showtimes.
     */
    public boolean isActiveForShowtimes() {
        return "Upcoming".equalsIgnoreCase(this.status) || "Now Showing".equalsIgnoreCase(this.status);
    }

    public boolean isArchived() {
        return "Archived".equalsIgnoreCase(this.status);
    }
}
