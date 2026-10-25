package com.cinema.dao;

import com.cinema.model.NewsArticle;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for public News data (UC04 – View Promotions &amp; News).
 *
 * Business rules applied in SQL:
 *  BR-PN-04 / BR-47  only PUBLISHED news whose publication time is not in the future
 *  BR-PN-05          Draft / Pending / Rejected / Archived news is never returned
 */
@Repository
public class NewsDAO {

    private final JdbcTemplate jdbcTemplate;

    public NewsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<NewsArticle> rowMapper = (rs, rowNum) -> mapNews(rs);

    private NewsArticle mapNews(ResultSet rs) throws SQLException {
        NewsArticle article = new NewsArticle();
        article.setNewsId(rs.getInt("NewsID"));
        int staffId = rs.getInt("CreatedByStaffID");
        if (!rs.wasNull()) article.setCreatedByStaffId(staffId);
        int revAccId = rs.getInt("ReviewedByAccountID");
        if (!rs.wasNull()) article.setReviewedByAccountId(revAccId);

        article.setTitle(rs.getString("Title"));
        article.setContent(rs.getString("Content"));
        article.setThumbnailUrl(rs.getString("ThumbnailURL"));
        article.setStatus(rs.getString("Status"));

        Timestamp created = rs.getTimestamp("CreatedAt");
        article.setCreatedAt(created != null ? created.toLocalDateTime() : null);

        Timestamp published = rs.getTimestamp("PublishedAt");
        article.setPublishedAt(published != null ? published.toLocalDateTime() : article.getCreatedAt());

        String author = rs.getString("AuthorName");
        if (author != null && !author.isBlank()) article.setAuthorName(author);

        return article;
    }

    private static final String NEWS_COLUMNS = """
            n.NewsID, n.CreatedByStaffID, n.ReviewedByAccountID,
            n.Title, n.Content, n.ThumbnailURL, n.[Status],
            n.CreatedAt, n.PublishedAt,
            COALESCE(acc.FullName, N'CINEORA Editorial Team') AS AuthorName
            """;

    private static final String NEWS_FROM = """
            FROM dbo.News n
            LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
            LEFT JOIN dbo.Account acc ON acc.AccountID = s.AccountID
            """;

    private static final String PUBLISHED_FILTER = """
            WHERE UPPER(n.[Status]) = 'PUBLISHED'
              AND COALESCE(n.PublishedAt, n.CreatedAt) <= SYSDATETIME()
            """;

    private static final String NEWEST_FIRST = " ORDER BY COALESCE(n.PublishedAt, n.CreatedAt) DESC";

    /** All published news, newest first. */
    public List<NewsArticle> findAllPublished() {
        String sql = "SELECT " + NEWS_COLUMNS + NEWS_FROM + PUBLISHED_FILTER + NEWEST_FIRST;
        return jdbcTemplate.query(sql, rowMapper);
    }

    /** The newest published news (Home Page + "other news" block). */
    public List<NewsArticle> findPublishedNews(int limit) {
        if (limit <= 0) {
            return findAllPublished();
        }
        String sql = "SELECT TOP (?) " + NEWS_COLUMNS + NEWS_FROM + PUBLISHED_FILTER + NEWEST_FIRST;
        return jdbcTemplate.query(sql, rowMapper, limit);
    }

    /**
     * One article only if it is published – unpublished news cannot be opened
     * by typing its ID in the URL (BR-47).
     */
    public Optional<NewsArticle> findPublishedById(int newsId) {
        String sql = "SELECT " + NEWS_COLUMNS + NEWS_FROM + PUBLISHED_FILTER + " AND n.NewsID = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, newsId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Any article by ID regardless of status (kept for back-office use). */
    public Optional<NewsArticle> findById(int newsId) {
        String sql = "SELECT " + NEWS_COLUMNS + NEWS_FROM + " WHERE n.NewsID = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, newsId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
