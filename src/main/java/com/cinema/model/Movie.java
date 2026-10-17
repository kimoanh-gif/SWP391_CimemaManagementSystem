package com.cinema.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Movie for customer & public catalog.
 * Screens 21 (Home), 22 (Movie List), 23 (Movie Details).
 */
public class Movie {

    private Integer movieId;
    private String title;
    private Integer duration; // in minutes
    private LocalDate releaseDate;
    private String language;
    private String director;
    private String ageRating; // P, T13, T16, T18
    private String description;
    private String status; // NOW_SHOWING, COMING_SOON, ACTIVE, ENDED, ARCHIVED
    private String posterUrl;
    private String trailerUrl;
    private String genres; // comma separated string e.g. "Hành Động, Phiêu Lưu"
    private List<Genre> genreList = new ArrayList<>();
    private Double averageRating = 5.0;
    private Integer reviewCount = 0;
    private Integer activeShowtimesCount = 0;

    public Movie() {
    }

    public Movie(Integer movieId, String title, Integer duration, LocalDate releaseDate,
                 String language, String director, String ageRating, String description,
                 String status, String posterUrl, String trailerUrl, String genres) {
        this.movieId = movieId;
        this.title = title;
        this.duration = duration;
        this.releaseDate = releaseDate;
        this.language = language;
        this.director = director;
        this.ageRating = ageRating;
        this.description = description;
        this.status = status;
        this.posterUrl = posterUrl;
        this.trailerUrl = trailerUrl;
        this.genres = genres;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDuration() {
        return duration != null ? duration : 0;
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

    public String getAgeRating() {
        return ageRating != null ? ageRating : "P";
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

    public String getStatus() {
        return status != null ? status : "NOW_SHOWING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPosterUrl() {
        if (posterUrl != null && !posterUrl.isBlank()) {
            return posterUrl;
        }
        return "/images/cinema-login-hero.png";
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getGenres() {
        return (genres != null && !genres.isBlank()) ? genres : "Phim truyện";
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public List<Genre> getGenreList() {
        return genreList;
    }

    public void setGenreList(List<Genre> genreList) {
        this.genreList = genreList;
    }

    public Double getAverageRating() {
        return averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 5.0;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount != null ? reviewCount : 0;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getActiveShowtimesCount() {
        return activeShowtimesCount != null ? activeShowtimesCount : 0;
    }

    public void setActiveShowtimesCount(Integer activeShowtimesCount) {
        this.activeShowtimesCount = activeShowtimesCount;
    }

    // Helper methods for Thymeleaf UI
    public boolean isNowShowing() {
        return "NOW_SHOWING".equalsIgnoreCase(status) || "Now Showing".equalsIgnoreCase(status);
    }

    public boolean isComingSoon() {
        return "COMING_SOON".equalsIgnoreCase(status) || "Upcoming".equalsIgnoreCase(status);
    }

    public String getFormattedReleaseDate() {
        if (releaseDate == null) return "Đang cập nhật";
        return releaseDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getAgeRatingBadgeClass() {
        if (ageRating == null) return "badge-p";
        return switch (ageRating.toUpperCase()) {
            case "P" -> "badge-p";
            case "T13", "C13", "13+" -> "badge-t13";
            case "T16", "C16", "16+" -> "badge-t16";
            case "T18", "C18", "18+" -> "badge-t18";
            default -> "badge-p";
        };
    }

    public String getAgeRatingDescription() {
        if (ageRating == null) return "Phù hợp cho mọi lứa tuổi";
        return switch (ageRating.toUpperCase()) {
            case "P" -> "P - Phim dành cho mọi đối tượng khán giả";
            case "T13", "C13", "13+" -> "T13 - Phim cấm phổ biến tới khán giả dưới 13 tuổi";
            case "T16", "C16", "16+" -> "T16 - Phim cấm phổ biến tới khán giả dưới 16 tuổi";
            case "T18", "C18", "18+" -> "T18 - Phim cấm phổ biến tới khán giả dưới 18 tuổi";
            default -> "Phim phổ biến cho khán giả";
        };
    }

    public String getEmbedTrailerUrl() {
        if (trailerUrl == null || trailerUrl.isBlank()) return "";
        if (trailerUrl.contains("watch?v=")) {
            return trailerUrl.replace("watch?v=", "embed/");
        } else if (trailerUrl.contains("youtu.be/")) {
            return trailerUrl.replace("youtu.be/", "www.youtube.com/embed/");
        }
        return trailerUrl;
    }
}
