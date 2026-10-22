package com.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NewsRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 500) String thumbnailUrl,
        @NotBlank String content,
        @NotBlank @Pattern(regexp = "(?i)DRAFT|PENDING") String status
) {
}
