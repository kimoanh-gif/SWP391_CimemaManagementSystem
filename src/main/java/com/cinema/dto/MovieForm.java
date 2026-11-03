package com.cinema.dto;

import com.cinema.model.Movie;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Form backing object and DTO for creating and updating movies.
 * Enforces field validations per SRS requirements.
 */
public class MovieForm {

    private Integer id;

    @NotBlank(message = "Movie title cannot be empty")
    @Size(max = 150, message = "Movie title cannot exceed 150 characters")
    private String title;

    @Size(max = 150, message = "Original title cannot exceed 150 characters")
    private String originalTitle;

    @NotNull(message = "Movie duration cannot be empty")
    @Min(value = 1, message = "Movie duration must be greater than 0 minutes")
    @Max(value = 600, message = "Movie duration cannot exceed 600 minutes")
    private Integer duration = 120;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 50, message = "Language cannot exceed 50 characters")
    private String language;

    @Size(max = 100, message = "Director cannot exceed 100 characters")
    private String director;

    @Size(max = 500, message = "Actors cannot exceed 500 characters")
    private String actors;

    @NotBlank(message = "Age rating cannot be empty")
    @Pattern(regexp = "^(P|K|T13|T16|T18)$", message = "Age rating must be one of: P, K, T13, T16, T18")
    private String ageRating = "P";

    @Size(max = 1000, message = "Synopsis cannot exceed 1000 characters")
    private String description;

    @Size(max = 500, message = "Poster URL cannot exceed 500 characters")
    private String poster;

    @Size(max = 500, message = "Banner URL cannot exceed 500 characters")
    private String banner;

    @Size(max = 500, message = "Trailer URL cannot exceed 500 characters")
    private String trailer;

    @NotBlank(message = "Movie status cannot be empty")
    @Pattern(regexp = "^(Upcoming|Now Showing|Ended|Archived)$", message = "Movie status must be Upcoming, Now Showing, Ended or Archived")
    private String status = "Upcoming";

    private List<@Positive(message = "Genre ID must be greater than 0") Integer> genreIds = new ArrayList<>();

    public MovieForm() {
    }

    @AssertTrue(message = "End date must be after release date")
    public boolean isValidDateRange() {
        if (releaseDate != null && endDate != null) {
            return !endDate.isBefore(releaseDate);
        }
        return true;
    }

    public static MovieForm from(Movie movie) {
        if (movie == null) {
            return new MovieForm();
        }
        MovieForm form = new MovieForm();
        form.setId(movie.getMovieId());
        form.setTitle(movie.getTitle());
        form.setOriginalTitle(movie.getOriginalTitle());
        form.setDuration(movie.getDuration());
        form.setReleaseDate(movie.getReleaseDate());
        form.setEndDate(movie.getEndDate());
        form.setLanguage(movie.getLanguage());
        form.setDirector(movie.getDirector());
        form.setActors(movie.getActors());
        form.setAgeRating(movie.getAgeRating() != null ? movie.getAgeRating() : "P");
        form.setDescription(movie.getDescription());
        form.setPoster(movie.getPoster());
        form.setBanner(movie.getBanner());
        form.setTrailer(movie.getTrailer());
        form.setStatus(movie.getStatus() != null ? movie.getStatus() : "Upcoming");
        form.setGenreIds(movie.getGenreIds());
        return form;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
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

    public List<Integer> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<Integer> genreIds) {
        this.genreIds = genreIds != null ? genreIds : new ArrayList<>();
    }
}
