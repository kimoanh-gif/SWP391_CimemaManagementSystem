package com.cinema.dao;

import com.cinema.model.ManagerNewsItem;
import com.cinema.model.NewsReviewLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * News queries and status transitions for the Manager review workflow (M07).
 * Every transition is a conditional UPDATE ("... WHERE Status = expected") so two managers
 * acting on the same article at the same time cannot overwrite each other.
 */
@Repository
public class ManagerNewsDAO {

    private static final Logger log = LoggerFactory.getLogger(ManagerNewsDAO.class);

    private static final String SELECT_NEWS = """
            SELECT n.NewsID, n.Title, n.Content, n.ThumbnailURL, n.[Status], n.RejectionReason,
                   n.CreatedAt, n.UpdatedAt, n.SubmittedAt, n.ReviewedAt, n.PublishedAt,
                   author.FullName AS AuthorName, s.Position AS AuthorPosition,
                   reviewer.FullName AS ReviewerName
            FROM dbo.News n
            LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
            LEFT JOIN dbo.Account author ON author.AccountID = s.AccountID
            LEFT JOIN dbo.Account reviewer ON reviewer.AccountID = n.ReviewedByAccountID
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ManagerNewsItem> newsMapper = (rs, i) -> {
        ManagerNewsItem item = new ManagerNewsItem();
        item.setNewsId(rs.getInt("NewsID"));
        item.setTitle(rs.getString("Title"));
        item.setContent(rs.getString("Content"));
        item.setThumbnailUrl(rs.getString("ThumbnailURL"));
        item.setStatus(rs.getString("Status"));
        item.setRejectionReason(rs.getString("RejectionReason"));
        item.setCreatedAt(toLocal(rs.getTimestamp("CreatedAt")));
        item.setUpdatedAt(toLocal(rs.getTimestamp("UpdatedAt")));
        item.setSubmittedAt(toLocal(rs.getTimestamp("SubmittedAt")));
        item.setReviewedAt(toLocal(rs.getTimestamp("ReviewedAt")));
        item.setPublishedAt(toLocal(rs.getTimestamp("PublishedAt")));
        item.setAuthorName(rs.getString("AuthorName"));
        item.setAuthorPosition(rs.getString("AuthorPosition"));
        item.setReviewerName(rs.getString("ReviewerName"));
        return item;
    };

    public ManagerNewsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Search criteria. {@code status} null = all; {@code since} null = any time. */
    public record Filter(String status, String keyword, LocalDateTime since) {
    }

    public int count(Filter filter) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(filter, args);
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.News n
                LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
                LEFT JOIN dbo.Account author ON author.AccountID = s.AccountID
                """ + where, Integer.class, args.toArray());
        return total == null ? 0 : total;
    }

    /** Pending articles first (oldest submission first), then the most recently changed ones. */
    public List<ManagerNewsItem> find(Filter filter, int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String sql = SELECT_NEWS + buildWhere(filter, args) + """
                 ORDER BY CASE WHEN UPPER(n.[Status]) = 'PENDING' THEN 0 ELSE 1 END,
                          CASE WHEN UPPER(n.[Status]) = 'PENDING' THEN COALESCE(n.SubmittedAt, n.CreatedAt) END ASC,
                          COALESCE(n.PublishedAt, n.ReviewedAt, n.SubmittedAt, n.UpdatedAt, n.CreatedAt) DESC,
                          n.NewsID DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        args.add(Math.max(0, offset));
        args.add(Math.max(1, limit));
        return jdbcTemplate.query(sql, newsMapper, args.toArray());
    }

    public Optional<ManagerNewsItem> findById(int newsId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SELECT_NEWS + " WHERE n.NewsID = ?", newsMapper, newsId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Number of articles per (upper-case) status. */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> counts = new HashMap<>();
        jdbcTemplate.query("SELECT UPPER([Status]) AS St, COUNT(*) AS Total FROM dbo.News GROUP BY UPPER([Status])",
                rs -> {
                    counts.put(rs.getString("St"), rs.getInt("Total"));
                });
        return counts;
    }

    public int countPendingSubmittedToday() {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.News
                WHERE UPPER([Status]) = 'PENDING'
                  AND CAST(COALESCE(SubmittedAt, CreatedAt) AS date) = CAST(SYSDATETIME() AS date)
                """, Integer.class);
        return value == null ? 0 : value;
    }

    // ---------------------------------------------------------------- transitions

    /** PENDING -> APPROVED, or straight to PUBLISHED when {@code publishNow}. */
    public boolean approve(int newsId, int reviewerAccountId, boolean publishNow) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = ?, ReviewedByAccountID = ?, ReviewedAt = SYSDATETIME(),
                    RejectionReason = NULL, UpdatedAt = SYSDATETIME(),
                    PublishedAt = CASE WHEN ? = 1 THEN SYSDATETIME() ELSE PublishedAt END
                WHERE NewsID = ? AND UPPER([Status]) = 'PENDING'
                """, publishNow ? "PUBLISHED" : "APPROVED", reviewerAccountId, publishNow ? 1 : 0, newsId) == 1;
    }

    /** PENDING -> REJECTED with a mandatory reason. */
    public boolean reject(int newsId, int reviewerAccountId, String reason) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'REJECTED', ReviewedByAccountID = ?, ReviewedAt = SYSDATETIME(),
                    RejectionReason = ?, UpdatedAt = SYSDATETIME()
                WHERE NewsID = ? AND UPPER([Status]) = 'PENDING'
                """, reviewerAccountId, reason, newsId) == 1;
    }

    /** APPROVED -> PUBLISHED. */
    public boolean publish(int newsId, int reviewerAccountId) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'PUBLISHED', PublishedAt = SYSDATETIME(), UpdatedAt = SYSDATETIME(),
                    ReviewedByAccountID = ?
                WHERE NewsID = ? AND UPPER([Status]) = 'APPROVED'
                """, reviewerAccountId, newsId) == 1;
    }

    /** PUBLISHED -> APPROVED (hidden from the public pages again). */
    public boolean unpublish(int newsId, int reviewerAccountId) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'APPROVED', PublishedAt = NULL, UpdatedAt = SYSDATETIME(),
                    ReviewedByAccountID = ?
                WHERE NewsID = ? AND UPPER([Status]) = 'PUBLISHED'
                """, reviewerAccountId, newsId) == 1;
    }

    /** REJECTED -> PENDING so the article goes back into the review queue. */
    public boolean reopen(int newsId) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'PENDING', SubmittedAt = SYSDATETIME(), UpdatedAt = SYSDATETIME()
                WHERE NewsID = ? AND UPPER([Status]) = 'REJECTED'
                """, newsId) == 1;
    }

    // ---------------------------------------------------------------- audit log

    /**
     * Writes one row to dbo.NewsReviewLog. Logging must never block a review, so a missing table
     * (07_manager_news_review.sql not run yet) is reported in the server log and ignored.
     */
    public void insertLog(int newsId, int accountId, String action, String fromStatus, String toStatus, String note) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO dbo.NewsReviewLog (NewsID, AccountID, [Action], FromStatus, ToStatus, Note)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, newsId, accountId, action, fromStatus, toStatus, note);
        } catch (DataAccessException e) {
            log.warn("Could not write NewsReviewLog (run database/07_manager_news_review.sql): {}", e.getMessage());
        }
    }

    public List<NewsReviewLogEntry> findRecentLogs(int limit) {
        try {
            return jdbcTemplate.query("""
                    SELECT TOP (%d) l.NewsID, l.[Action], l.Note, l.CreatedAt, n.Title, a.FullName
                    FROM dbo.NewsReviewLog l
                    JOIN dbo.News n ON n.NewsID = l.NewsID
                    LEFT JOIN dbo.Account a ON a.AccountID = l.AccountID
                    ORDER BY l.CreatedAt DESC, l.NewsReviewLogID DESC
                    """.formatted(Math.max(1, limit)), (rs, i) -> {
                NewsReviewLogEntry entry = new NewsReviewLogEntry();
                entry.setNewsId(rs.getInt("NewsID"));
                entry.setAction(rs.getString("Action"));
                entry.setNote(rs.getString("Note"));
                entry.setCreatedAt(toLocal(rs.getTimestamp("CreatedAt")));
                entry.setNewsTitle(rs.getString("Title"));
                entry.setReviewerName(rs.getString("FullName"));
                return entry;
            });
        } catch (DataAccessException e) {
            log.warn("Could not read NewsReviewLog (run database/07_manager_news_review.sql): {}", e.getMessage());
            return List.of();
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String buildWhere(Filter filter, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (filter == null) return where.toString();
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND UPPER(n.[Status]) = ?");
            args.add(filter.status().toUpperCase(Locale.ROOT));
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            String like = "%" + escapeLike(filter.keyword().trim()) + "%";
            where.append(" AND (n.Title LIKE ? ESCAPE '\\' OR author.FullName LIKE ? ESCAPE '\\'");
            args.add(like);
            args.add(like);
            String digits = filter.keyword().replaceAll("\\D", "");
            if (!digits.isEmpty() && digits.length() <= 9) {
                where.append(" OR n.NewsID = ?");
                args.add(Integer.parseInt(digits));
            }
            where.append(")");
        }
        if (filter.since() != null) {
            where.append(" AND COALESCE(n.UpdatedAt, n.SubmittedAt, n.CreatedAt) >= ?");
            args.add(Timestamp.valueOf(filter.since()));
        }
        return where.toString();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[");
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
