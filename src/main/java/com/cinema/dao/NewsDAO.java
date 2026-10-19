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

        try {
            String author = rs.getString("AuthorName");
            if (author != null && !author.isBlank()) article.setAuthorName(author);
        } catch (SQLException ignored) {
        }

        return article;
    }

    private static final String BASE_NEWS_SQL = """
            SELECT n.NewsID, n.CreatedByStaffID, n.ReviewedByAccountID,
                   n.Title, n.Content, n.ThumbnailURL, n.[Status],
                   n.CreatedAt, n.PublishedAt,
                   COALESCE(acc.FullName, N'Ban Biên Tập CineVerse') AS AuthorName
            FROM dbo.News n
            LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
            LEFT JOIN dbo.Account acc ON acc.AccountID = s.AccountID
            """;

    public List<NewsArticle> findPublishedNews(int limit) {
        String sql = BASE_NEWS_SQL + """
                WHERE UPPER(n.[Status]) = 'PUBLISHED'
                ORDER BY COALESCE(n.PublishedAt, n.CreatedAt) DESC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<NewsArticle> findAllPublished() {
        String sql = BASE_NEWS_SQL + """
                WHERE UPPER(n.[Status]) = 'PUBLISHED'
                ORDER BY COALESCE(n.PublishedAt, n.CreatedAt) DESC
                """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<NewsArticle> findById(int newsId) {
        String sql = BASE_NEWS_SQL + " WHERE n.NewsID = ?";
        try {
            NewsArticle article = jdbcTemplate.queryForObject(sql, rowMapper, newsId);
            return Optional.ofNullable(article);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
