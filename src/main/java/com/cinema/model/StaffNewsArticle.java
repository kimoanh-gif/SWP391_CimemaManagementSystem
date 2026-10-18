package com.cinema.model;

import java.time.LocalDateTime;

public record StaffNewsArticle(
        Integer newsId,
        Integer createdByStaffId,
        String title,
        String summary,
        String content,
        String thumbnailUrl,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt
) {
}
