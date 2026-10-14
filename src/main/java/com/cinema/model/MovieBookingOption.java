package com.cinema.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovieBookingOption {
    private int movieId;
    private String title;
    private int duration;
    private LocalDate releaseDate;
    private String language;
    private String director;
    private String ageRating;
    private String description;
    private String posterUrl;
    private String genres;
    private List<ShowtimeOption> showtimes = new ArrayList<>();

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
    public List<ShowtimeOption> getShowtimes() { return showtimes; }
    public void setShowtimes(List<ShowtimeOption> showtimes) { this.showtimes = showtimes; }
}
