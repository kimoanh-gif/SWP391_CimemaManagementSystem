package com.cinema.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 150, message = "Tên phim không được vượt quá 150 ký tự")
    private String title;

    @Size(max = 150, message = "Tên gốc không được vượt quá 150 ký tự")
    private String originalTitle;

    @NotNull(message = "Thời lượng phim không được để trống")
    @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0 phút")
    @Max(value = 600, message = "Thời lượng phim không được vượt quá 600 phút")
    private Integer duration = 120;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 50, message = "Ngôn ngữ không được vượt quá 50 ký tự")
    private String language;

    @Size(max = 100, message = "Đạo diễn không được vượt quá 100 ký tự")
    private String director;

    @Size(max = 500, message = "Diễn viên không được vượt quá 500 ký tự")
    private String actors;

    @NotBlank(message = "Độ tuổi quy định không được để trống")
    @Pattern(regexp = "^(P|K|T13|T16|T18)$", message = "Độ tuổi quy định phải là một trong các giá trị: P, K, T13, T16, T18")
    private String ageRating = "P";

    @Size(max = 1000, message = "Tóm tắt phim không được vượt quá 1000 ký tự")
    private String description;

    @Size(max = 500, message = "Đường dẫn Poster không được vượt quá 500 ký tự")
    private String poster;

    @Size(max = 500, message = "Đường dẫn Trailer không được vượt quá 500 ký tự")
    private String trailer;

    @NotBlank(message = "Trạng thái phim không được để trống")
    @Pattern(regexp = "^(Upcoming|Now Showing|Ended|Archived)$", message = "Trạng thái phim phải là Upcoming, Now Showing, Ended hoặc Archived")
    private String status = "Upcoming";

    private List<Integer> genreIds = new ArrayList<>();

    public MovieForm() {
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
