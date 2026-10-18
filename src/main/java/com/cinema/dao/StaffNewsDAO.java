package com.cinema.dao;

import com.cinema.model.StaffNewsArticle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StaffNewsDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<StaffNewsArticle> rowMapper = (rs, rowNum) -> new StaffNewsArticle(
            rs.getInt("NewsID"),
            rs.getInt("CreatedByStaffID"),
            rs.getString("Title"),
            rs.getString("Summary"),
            rs.getString("Content"),
            rs.getString("ThumbnailURL"),
            rs.getString("Status"),
            rs.getTimestamp("CreatedAt") == null ? null : rs.getTimestamp("CreatedAt").toLocalDateTime(),
            rs.getTimestamp("UpdatedAt") == null ? null : rs.getTimestamp("UpdatedAt").toLocalDateTime(),
            rs.getTimestamp("SubmittedAt") == null ? null : rs.getTimestamp("SubmittedAt").toLocalDateTime()
    );

    public StaffNewsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Integer> findActiveStaffId(int accountId) {
        return jdbcTemplate.query("""
                SELECT StaffID FROM dbo.Staff
                WHERE AccountID = ? AND UPPER([Status]) = 'ACTIVE'
                """, (rs, rowNum) -> rs.getInt("StaffID"), accountId).stream().findFirst();
    }

    public Integer insert(int staffId, String title, String summary, String thumbnailUrl,
                          String content, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO dbo.News
                    (CreatedByStaffID, Title, Summary, ThumbnailURL, Content, [Status], SubmittedAt)
                OUTPUT inserted.NewsID
                VALUES (?, ?, ?, ?, ?, ?, CASE WHEN ? = 'PENDING' THEN SYSDATETIME() ELSE NULL END)
                """, Integer.class, staffId, title, summary, thumbnailUrl, content, status, status);
    }

    public List<StaffNewsArticle> findAllByStaffId(int staffId) {
        return jdbcTemplate.query("""
                SELECT NewsID, CreatedByStaffID, Title, Summary, Content, ThumbnailURL,
                       [Status], CreatedAt, UpdatedAt, SubmittedAt
                FROM dbo.News WHERE CreatedByStaffID = ?
                ORDER BY COALESCE(UpdatedAt, CreatedAt) DESC
                """, rowMapper, staffId);
    }

    public List<StaffNewsArticle> findAll() {
        return jdbcTemplate.query("""
                SELECT NewsID, CreatedByStaffID, Title, Summary, Content, ThumbnailURL,
                       [Status], CreatedAt, UpdatedAt, SubmittedAt
                FROM dbo.News ORDER BY COALESCE(UpdatedAt, CreatedAt) DESC
                """, rowMapper);
    }

    public Optional<StaffNewsArticle> findById(int newsId) {
        return jdbcTemplate.query("""
                SELECT NewsID, CreatedByStaffID, Title, Summary, Content, ThumbnailURL,
                       [Status], CreatedAt, UpdatedAt, SubmittedAt
                FROM dbo.News WHERE NewsID = ?
                """, rowMapper, newsId).stream().findFirst();
    }

    public int update(int newsId, Integer ownerStaffId, boolean allowOtherOwners,
                      String title, String summary, String thumbnailUrl, String content, String status) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET Title = ?, Summary = ?, ThumbnailURL = ?, Content = ?, [Status] = ?,
                    UpdatedAt = SYSDATETIME(),
                    SubmittedAt = CASE WHEN ? = 'PENDING' THEN SYSDATETIME() ELSE NULL END,
                    ReviewedByAccountID = NULL, ReviewedAt = NULL, RejectionReason = NULL
                WHERE NewsID = ? AND UPPER([Status]) IN ('DRAFT', 'REJECTED')
                  AND (? = 1 OR CreatedByStaffID = ?)
                """, title, summary, thumbnailUrl, content, status, status, newsId,
                allowOtherOwners ? 1 : 0, ownerStaffId);
    }
}
