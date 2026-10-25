package com.cinema.dao;

import com.cinema.model.ManagerNewsFilter;
import com.cinema.model.ManagerNewsItem;
import com.cinema.model.ManagerNewsStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO for Manager News Management (M07 / UC35 Review &amp; Approve News).
 * Assignee: Nguyễn Đức Anh (Iter3)
 *
 * Status changes always check the current status in the WHERE clause, so a news item
 * can only be approved / rejected while it is PENDING (BR-45, BR-46).
 */
@Repository
public class ManagerNewsDAO {

    private static final Pattern NEWS_CODE = Pattern.compile("^#?(?:NEWS-)?0*(\\d{1,9})$", Pattern.CASE_INSENSITIVE);

    private static final String SELECT_FROM = """
            SELECT n.NewsID, n.Title, n.Content, n.ThumbnailURL, n.[Status],
                   n.CreatedAt, n.UpdatedAt, n.SubmittedAt, n.ReviewedAt, n.PublishedAt, n.RejectionReason,
                   a.FullName AS AuthorName, s.Position AS AuthorPosition, r.FullName AS ReviewerName
            FROM dbo.News n
            LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
            LEFT JOIN dbo.Account a ON a.AccountID = s.AccountID
            LEFT JOIN dbo.Account r ON r.AccountID = n.ReviewedByAccountID
            """;

    private static final String COUNT_FROM = """
            SELECT COUNT(*)
            FROM dbo.News n
            LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
            LEFT JOIN dbo.Account a ON a.AccountID = s.AccountID
            """;

    private final JdbcTemplate jdbcTemplate;

    public ManagerNewsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ManagerNewsItem> mapper = (rs, rowNum) -> {
        ManagerNewsItem item = new ManagerNewsItem();
        item.setNewsId(rs.getInt("NewsID"));
        item.setTitle(rs.getString("Title"));
        item.setContent(rs.getString("Content"));
        item.setThumbnailUrl(rs.getString("ThumbnailURL"));
        item.setStatus(rs.getString("Status"));
        item.setCreatedAt(toLocal(rs, "CreatedAt"));
        item.setUpdatedAt(toLocal(rs, "UpdatedAt"));
        item.setSubmittedAt(toLocal(rs, "SubmittedAt"));
        item.setReviewedAt(toLocal(rs, "ReviewedAt"));
        item.setPublishedAt(toLocal(rs, "PublishedAt"));
        item.setRejectionReason(rs.getString("RejectionReason"));
        item.setAuthorName(rs.getString("AuthorName"));
        item.setAuthorPosition(rs.getString("AuthorPosition"));
        item.setReviewerName(rs.getString("ReviewerName"));
        return item;
    };

    // ------------------------------------------------------------------ queries

    public List<ManagerNewsItem> search(ManagerNewsFilter filter) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(filter, args);
        String sql = SELECT_FROM + where + """
                 ORDER BY CASE WHEN UPPER(n.[Status]) = 'PENDING' THEN 0 ELSE 1 END,
                          COALESCE(n.SubmittedAt, n.UpdatedAt, n.CreatedAt) DESC, n.NewsID DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        args.add(filter.offset());
        args.add(filter.size());
        return jdbcTemplate.query(sql, mapper, args.toArray());
    }

    public int count(ManagerNewsFilter filter) {
        List<Object> args = new ArrayList<>();
        String sql = COUNT_FROM + buildWhere(filter, args);
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, args.toArray());
        return total != null ? total : 0;
    }

    public ManagerNewsStats stats() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS Total,
                       SUM(CASE WHEN UPPER([Status]) = 'PENDING' THEN 1 ELSE 0 END) AS Pending,
                       SUM(CASE WHEN UPPER([Status]) = 'PENDING'
                                 AND COALESCE(SubmittedAt, CreatedAt) < DATEADD(HOUR, -24, SYSDATETIME())
                                THEN 1 ELSE 0 END) AS UrgentPending,
                       SUM(CASE WHEN UPPER([Status]) = 'PUBLISHED' AND PublishedAt > SYSDATETIME() THEN 1 ELSE 0 END) AS Approved,
                       SUM(CASE WHEN UPPER([Status]) = 'REJECTED' THEN 1 ELSE 0 END) AS Rejected,
                       SUM(CASE WHEN UPPER([Status]) = 'PUBLISHED'
                                 AND (PublishedAt IS NULL OR PublishedAt <= SYSDATETIME()) THEN 1 ELSE 0 END) AS Publishing,
                       SUM(CASE WHEN UPPER([Status]) = 'DRAFT' THEN 1 ELSE 0 END) AS Drafts,
                       SUM(CASE WHEN UPPER([Status]) = 'ARCHIVED' THEN 1 ELSE 0 END) AS Archived
                FROM dbo.News
                """, (rs, rowNum) -> new ManagerNewsStats(
                rs.getInt("Total"), rs.getInt("Pending"), rs.getInt("UrgentPending"),
                rs.getInt("Approved"), rs.getInt("Rejected"), rs.getInt("Publishing"),
                rs.getInt("Drafts"), rs.getInt("Archived")));
    }

    public Optional<ManagerNewsItem> findById(int newsId) {
        return jdbcTemplate.query(SELECT_FROM + " WHERE n.NewsID = ?", mapper, newsId).stream().findFirst();
    }

    /** Latest review actions for the "News review history" panel. */
    public List<ManagerNewsItem> findReviewHistory(int limit) {
        String sql = """
                SELECT TOP (?) n.NewsID, n.Title, n.Content, n.ThumbnailURL, n.[Status],
                       n.CreatedAt, n.UpdatedAt, n.SubmittedAt, n.ReviewedAt, n.PublishedAt, n.RejectionReason,
                       a.FullName AS AuthorName, s.Position AS AuthorPosition, r.FullName AS ReviewerName
                FROM dbo.News n
                LEFT JOIN dbo.Staff s ON s.StaffID = n.CreatedByStaffID
                LEFT JOIN dbo.Account a ON a.AccountID = s.AccountID
                LEFT JOIN dbo.Account r ON r.AccountID = n.ReviewedByAccountID
                WHERE n.ReviewedAt IS NOT NULL
                ORDER BY n.ReviewedAt DESC
                """;
        return jdbcTemplate.query(sql, mapper, limit);
    }

    // ------------------------------------------------------------------ status changes

    /** PENDING → PUBLISHED. publishAt in the future = approved and scheduled. */
    public int approve(int newsId, int managerAccountId, LocalDateTime publishAt) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'PUBLISHED', ReviewedByAccountID = ?, ReviewedAt = SYSDATETIME(),
                    PublishedAt = COALESCE(?, SYSDATETIME()), RejectionReason = NULL
                WHERE NewsID = ? AND UPPER([Status]) = 'PENDING'
                """, managerAccountId, publishAt != null ? Timestamp.valueOf(publishAt) : null, newsId);
    }

    /** PENDING → REJECTED with a reason (the author can edit and resubmit – BR-48). */
    public int reject(int newsId, int managerAccountId, String reason) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'REJECTED', ReviewedByAccountID = ?, ReviewedAt = SYSDATETIME(),
                    RejectionReason = ?, PublishedAt = NULL
                WHERE NewsID = ? AND UPPER([Status]) = 'PENDING'
                """, managerAccountId, reason, newsId);
    }

    /** PUBLISHED → ARCHIVED (removed from the public News page). */
    public int archive(int newsId, int managerAccountId) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET [Status] = 'ARCHIVED', ReviewedByAccountID = ?, ReviewedAt = SYSDATETIME()
                WHERE NewsID = ? AND UPPER([Status]) = 'PUBLISHED'
                """, managerAccountId, newsId);
    }

    /** Scheduled (approved) news → publish now. */
    public int publishNow(int newsId, int managerAccountId) {
        return jdbcTemplate.update("""
                UPDATE dbo.News
                SET PublishedAt = SYSDATETIME(), ReviewedByAccountID = ?, ReviewedAt = SYSDATETIME()
                WHERE NewsID = ? AND UPPER([Status]) = 'PUBLISHED' AND PublishedAt > SYSDATETIME()
                """, managerAccountId, newsId);
    }

    // ------------------------------------------------------------------ helpers

    private static String buildWhere(ManagerNewsFilter filter, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        switch (filter.status()) {
            case "PENDING", "REJECTED", "DRAFT", "ARCHIVED" -> {
                where.append(" AND UPPER(n.[Status]) = ?");
                args.add(filter.status());
            }
            case "APPROVED" -> where.append(" AND UPPER(n.[Status]) = 'PUBLISHED' AND n.PublishedAt > SYSDATETIME()");
            case "PUBLISHED" -> where.append(" AND UPPER(n.[Status]) = 'PUBLISHED' AND (n.PublishedAt IS NULL OR n.PublishedAt <= SYSDATETIME())");
            default -> { /* ALL */ }
        }
        switch (filter.period()) {
            case "WEEK" -> where.append(" AND n.CreatedAt >= DATEADD(DAY, -7, SYSDATETIME())");
            case "30DAYS" -> where.append(" AND n.CreatedAt >= DATEADD(DAY, -30, SYSDATETIME())");
            case "MONTH" -> where.append(" AND n.CreatedAt >= DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1)");
            default -> { /* ALL */ }
        }
        String keyword = filter.keyword();
        if (keyword != null && !keyword.isBlank()) {
            Matcher code = NEWS_CODE.matcher(keyword.trim());
            if (code.matches()) {
                where.append(" AND n.NewsID = ?");
                args.add(Integer.parseInt(code.group(1)));
            } else {
                String like = "%" + escapeLike(keyword.trim()) + "%";
                where.append(" AND (n.Title LIKE ? OR a.FullName LIKE ?)");
                args.add(like);
                args.add(like);
            }
        }
        return where.toString();
    }

    /** SQL Server LIKE wildcards are escaped with brackets. */
    static String escapeLike(String value) {
        return value.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    private static LocalDateTime toLocal(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
