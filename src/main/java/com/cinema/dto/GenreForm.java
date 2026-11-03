package com.cinema.dto;

import com.cinema.model.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form backing object & DTO for Genre creation and update operations.
 * Enforces field validations per SRS requirements.
 */
public class GenreForm {

    private Integer id;

    @NotBlank(message = "Genre name cannot be empty")
    @Size(min = 2, max = 100, message = "Genre name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Status cannot be empty")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    private String status = "ACTIVE";

    public GenreForm() {
    }

    public GenreForm(Integer id, String name, String description, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public static GenreForm from(Genre genre) {
        if (genre == null) {
            return new GenreForm();
        }
        return new GenreForm(
                genre.getGenreId(),
                genre.getName(),
                genre.getDescription(),
                genre.getStatus() != null ? genre.getStatus() : "ACTIVE"
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
