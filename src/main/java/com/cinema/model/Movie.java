package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified Movie entity supporting Customer Catalog and Manager Module operations.
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
    private String heroImageUrl;
    private String trailer;
    private String status; // 'Upcoming', 'Now Showing', 'Ended', 'Archived'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Genre> genres = new ArrayList<>();
    private int showtimeCount;
    private Double averageRating = 5.0;
    private Integer reviewCount = 0;
    private Integer activeShowtimesCount = 0;

    public Movie() {
    }

    public Movie(Integer movieId, String title, Integer duration, LocalDate releaseDate,
                 String language, String director, String ageRating, String description,
                 String status, String posterUrl, String trailerUrl, String genresStr) {
        this.movieId = movieId;
        this.title = title;
        this.duration = duration;
        this.releaseDate = releaseDate;
        this.language = language;
        this.director = director;
        this.ageRating = ageRating;
        this.description = description;
        this.status = status;
        this.poster = posterUrl;
        this.trailer = trailerUrl;
    }

    public Movie(int movieId, String title, int duration, String status) {
        this.movieId = movieId;
        this.title = title;
        this.duration = duration;
        this.status = status;
    }

    public Integer getId() {
        return movieId;
    }

    public void setId(Integer id) {
        this.movieId = id;
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

    public String getPosterUrl() {
        return poster != null ? poster : "";
    }

    public void setPosterUrl(String posterUrl) {
        this.poster = posterUrl;
    }

    public String getHeroImageUrl() {
        return heroImageUrl == null || heroImageUrl.isBlank() ? poster : heroImageUrl;
    }

    public void setHeroImageUrl(String heroImageUrl) {
        this.heroImageUrl = heroImageUrl;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public String getTrailerUrl() {
        return trailer != null ? trailer : "";
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailer = trailerUrl;
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

    public List<Genre> getGenreList() {
        return genres;
    }

    public void setGenreList(List<Genre> genreList) {
        this.genres = genreList != null ? genreList : new ArrayList<>();
    }

    public String getGenresFormatted() {
        if (genres == null || genres.isEmpty()) {
            return "";
        }
        return genres.stream().map(Genre::getName).collect(Collectors.joining(", "));
    }

    public String getGenreNames() {
        return getGenresFormatted();
    }

    public String getGenreNamesString() {
        if (genres == null || genres.isEmpty()) {
            return "Chưa phân loại";
        }
        return genres.stream()
                .map(Genre::getName)
                .collect(Collectors.joining(", "));
    }

    public String getGenreIdsString() {
        if (genres == null || genres.isEmpty()) {
            return "";
        }
        return genres.stream()
                .map(g -> String.valueOf(g.getGenreId()))
                .collect(Collectors.joining(","));
    }

    public int getShowtimeCount() {
        return showtimeCount;
    }

    public void setShowtimeCount(int showtimeCount) {
        this.showtimeCount = showtimeCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getActiveShowtimesCount() {
        return activeShowtimesCount;
    }

    public void setActiveShowtimesCount(Integer activeShowtimesCount) {
        this.activeShowtimesCount = activeShowtimesCount;
    }

    public String getFormattedReleaseDate() {
        return releaseDate != null ? releaseDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    public String getFormattedDuration() {
        if (duration == null || duration <= 0) return "";
        int h = duration / 60;
        int m = duration % 60;
        return h > 0 ? (h + "h " + m + "m") : (m + "m");
    }

    public List<Integer> getGenreIds() {
        if (genres == null) return new ArrayList<>();
        return genres.stream().map(Genre::getGenreId).collect(Collectors.toList());
    }

    public boolean isActiveForShowtimes() {
        return "Upcoming".equalsIgnoreCase(status) || "Now Showing".equalsIgnoreCase(status)
                || "NOW_SHOWING".equalsIgnoreCase(status) || "COMING_SOON".equalsIgnoreCase(status);
    }

    public boolean isNowShowing() {
        return "Now Showing".equalsIgnoreCase(status) || "NOW_SHOWING".equalsIgnoreCase(status);
    }

    public boolean isUpcoming() {
        return "Upcoming".equalsIgnoreCase(status) || "COMING_SOON".equalsIgnoreCase(status);
    }

    public boolean isComingSoon() {
        return isUpcoming();
    }

    public boolean isEnded() {
        return "Ended".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status);
    }

    public boolean isArchived() {
        return "Archived".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status);
    }

    public String getAgeRatingBadgeClass() {
        if (ageRating == null) return "badge-p";
        return switch (ageRating.toUpperCase().trim()) {
            case "P" -> "badge-p";
            case "K" -> "badge-k";
            case "T13", "C13", "13+" -> "badge-t13";
            case "T16", "C16", "16+" -> "badge-t16";
            case "T18", "C18", "18+" -> "badge-t18";
            default -> "badge-p";
        };
    }

    public String getAgeRatingDescription() {
        if (ageRating == null) return "Suitable for all audiences";
        return switch (ageRating.toUpperCase().trim()) {
            case "P" -> "P — Suitable for all audiences";
            case "K" -> "K — Viewers under 13 require parental guidance";
            case "T13", "C13", "13+" -> "T13 — Not suitable for viewers under 13";
            case "T16", "C16", "16+" -> "T16 — Not suitable for viewers under 16";
            case "T18", "C18", "18+" -> "T18 — Not suitable for viewers under 18";
            default -> "General audience release";
        };
    }

    public String getEmbedTrailerUrl() {
        String t = (trailer != null && !trailer.isBlank()) ? trailer : "";
        if (t.isBlank()) return "";
        if (t.contains("watch?v=")) {
            String videoId = t.substring(t.indexOf("watch?v=") + 8).split("[&?#]")[0];
            return "https://www.youtube.com/embed/" + videoId;
        } else if (t.contains("youtu.be/")) {
            String videoId = t.substring(t.indexOf("youtu.be/") + 9).split("[?&#]")[0];
            return "https://www.youtube.com/embed/" + videoId;
        }
        return t;
    }
}
