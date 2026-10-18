package com.cinema.model;

/**
 * Lightweight representation of a Movie associated with a Genre.
 */
public class MovieSummary {

    private Integer movieId;
    private String title;
    private String status;

    public MovieSummary() {
    }

    public MovieSummary(Integer movieId, String title, String status) {
        this.movieId = movieId;
        this.title = title;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
