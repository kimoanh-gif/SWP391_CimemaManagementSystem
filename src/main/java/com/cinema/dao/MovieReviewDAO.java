package com.cinema.dao;

import com.cinema.model.MovieReview;
import com.cinema.model.ReviewableMovie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class MovieReviewDAO {

    private final JdbcTemplate jdbcTemplate;

    public MovieReviewDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<MovieReview> rowMapper = (rs, rowNum) -> mapReview(rs);

    private MovieReview mapReview(ResultSet rs) throws SQLException {
        MovieReview r = new MovieReview();
        r.setReviewId(rs.getInt("ReviewID"));
        r.setCustomerId(rs.getInt("CustomerID"));
        r.setMovieId(rs.getInt("MovieID"));
        r.setRating(rs.getInt("Rating"));
        r.setContent(rs.getString("Content"));
        r.setStatus(rs.getString("Status"));

        Timestamp ts = rs.getTimestamp("CreatedAt");
        r.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);

        r.setCustomerName(rs.getString("CustomerName"));
        return r;
    }

    public List<MovieReview> findByMovieId(int movieId) {
        String sql = """
                SELECT r.ReviewID, r.CustomerID, r.MovieID, r.Rating, r.Content, r.[Status], r.CreatedAt,
                       COALESCE(acc.FullName, N'Khách hàng ẩn danh') AS CustomerName
                FROM dbo.MovieReview r
                JOIN dbo.Customer c ON c.CustomerID = r.CustomerID
                JOIN dbo.Account acc ON acc.AccountID = c.AccountID
                WHERE r.MovieID = ?
                  AND UPPER(r.[Status]) = 'ACTIVE'
                ORDER BY r.CreatedAt DESC
                """;
        return jdbcTemplate.query(sql, rowMapper, movieId);
    }

    public boolean hasCustomerReviewed(int customerId, int movieId) {
        String sql = "SELECT COUNT(*) FROM dbo.MovieReview WHERE CustomerID = ? AND MovieID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, customerId, movieId);
        return count != null && count > 0;
    }

    public boolean canCustomerReview(int accountId, int movieId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID=st.MovieID
                WHERE c.AccountID=? AND m.MovieID=? AND b.[Status]='CONFIRMED'
                  AND DATEADD(MINUTE,m.Duration,st.StartTime) <= SYSDATETIME()
                """, Integer.class, accountId, movieId);
        return count != null && count > 0;
    }

    public List<ReviewableMovie> findReviewableMovies(int accountId) {
        return jdbcTemplate.query("""
                WITH eligible AS (
                    SELECT m.MovieID,m.Title,m.PosterURL,b.BookingCode,st.StartTime,
                           ROW_NUMBER() OVER(PARTITION BY m.MovieID ORDER BY st.StartTime DESC) row_number
                    FROM dbo.Booking b
                    JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                    JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID
                    JOIN dbo.Movie m ON m.MovieID=st.MovieID
                    WHERE c.AccountID=? AND UPPER(b.[Status])='CONFIRMED'
                      AND DATEADD(MINUTE,m.Duration,st.StartTime)<=SYSDATETIME()
                      AND EXISTS(SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PAID')
                )
                SELECT e.MovieID,e.Title,e.PosterURL,e.BookingCode,e.StartTime,r.Rating,r.Content
                FROM eligible e
                LEFT JOIN dbo.Customer c ON c.AccountID=?
                LEFT JOIN dbo.MovieReview r ON r.CustomerID=c.CustomerID AND r.MovieID=e.MovieID AND UPPER(r.[Status])='ACTIVE'
                WHERE e.row_number=1 ORDER BY e.StartTime DESC
                """, (rs,n) -> new ReviewableMovie(rs.getInt("MovieID"),rs.getString("Title"),rs.getString("PosterURL"),
                rs.getString("BookingCode"),rs.getObject("StartTime",java.time.LocalDateTime.class),
                (Integer)rs.getObject("Rating"),rs.getString("Content")), accountId, accountId);
    }

    public void saveOrUpdateReview(int customerId, int movieId, int rating, String content) {
        if (hasCustomerReviewed(customerId, movieId)) {
            String updateSql = """
                    UPDATE dbo.MovieReview
                    SET Rating = ?, Content = ?, CreatedAt = SYSDATETIME(), [Status] = 'ACTIVE'
                    WHERE CustomerID = ? AND MovieID = ?
                    """;
            jdbcTemplate.update(updateSql, rating, content, customerId, movieId);
        } else {
            String insertSql = """
                    INSERT INTO dbo.MovieReview (CustomerID, MovieID, Rating, Content, [Status], CreatedAt)
                    VALUES (?, ?, ?, ?, 'ACTIVE', SYSDATETIME())
                    """;
            jdbcTemplate.update(insertSql, customerId, movieId, rating, content);
        }
    }
}
