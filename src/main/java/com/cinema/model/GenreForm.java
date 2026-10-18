package com.cinema.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form backing object & DTO for Genre creation and update operations.
 * Enforces field validations per SRS requirements.
 */
public class GenreForm {

    private Integer id;

    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(min = 2, max = 100, message = "Tên thể loại phải từ 2 đến 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
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
