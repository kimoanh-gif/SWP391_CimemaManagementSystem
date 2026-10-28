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
import org.springframework.dao.DuplicateKeyException;

@Repository
public class MovieReviewDAO {
    private final JdbcTemplate jdbcTemplate;
    public MovieReviewDAO(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    private final RowMapper<MovieReview> rowMapper = (rs, n) -> mapReview(rs);

    private MovieReview mapReview(ResultSet rs) throws SQLException {
        MovieReview r = new MovieReview();
        r.setReviewId(rs.getInt("ReviewID")); r.setCustomerId(rs.getInt("CustomerID"));
        r.setMovieId(rs.getInt("MovieID")); r.setBookingId((Integer) rs.getObject("BookingID"));
        r.setRating(rs.getInt("Rating")); r.setContent(rs.getString("Content")); r.setStatus(rs.getString("Status"));
        Timestamp ts = rs.getTimestamp("CreatedAt"); r.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        r.setCustomerName(rs.getString("CustomerName")); return r;
    }

    public List<MovieReview> findByMovieId(int movieId) {
        return jdbcTemplate.query("""
                SELECT r.ReviewID,r.CustomerID,r.MovieID,r.BookingID,r.Rating,r.Content,r.[Status],r.CreatedAt,
                       COALESCE(a.FullName,N'CINEORA Guest') CustomerName
                FROM dbo.MovieReview r JOIN dbo.Customer c ON c.CustomerID=r.CustomerID
                JOIN dbo.Account a ON a.AccountID=c.AccountID
                WHERE r.MovieID=? AND UPPER(r.[Status])='ACTIVE' ORDER BY r.CreatedAt DESC
                """, rowMapper, movieId);
    }

    public Integer findEligibleBookingId(int accountId, int movieId, Integer requestedBookingId) {
        List<Integer> ids = jdbcTemplate.query("""
                SELECT TOP 1 b.BookingID FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID JOIN dbo.Movie m ON m.MovieID=st.MovieID
                WHERE c.AccountID=? AND m.MovieID=? AND UPPER(b.[Status])='CONFIRMED'
                  AND DATEADD(MINUTE,m.Duration,st.StartTime)<=SYSDATETIME()
                  AND EXISTS(SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PAID')
                  AND (? IS NULL OR b.BookingID=?)
                ORDER BY CASE WHEN EXISTS(SELECT 1 FROM dbo.MovieReview r WHERE r.BookingID=b.BookingID) THEN 1 ELSE 0 END,
                         st.StartTime DESC
                """, (rs, n) -> rs.getInt(1), accountId, movieId, requestedBookingId, requestedBookingId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public MovieReview findByBookingId(int bookingId) {
        List<MovieReview> reviews = jdbcTemplate.query("""
                SELECT r.ReviewID,r.CustomerID,r.MovieID,r.BookingID,r.Rating,r.Content,r.[Status],r.CreatedAt,
                       COALESCE(a.FullName,N'CINEORA Guest') CustomerName
                FROM dbo.MovieReview r JOIN dbo.Customer c ON c.CustomerID=r.CustomerID
                JOIN dbo.Account a ON a.AccountID=c.AccountID WHERE r.BookingID=?
                """, rowMapper, bookingId);
        return reviews.isEmpty() ? null : reviews.get(0);
    }

    public List<ReviewableMovie> findReviewableMovies(int accountId) {
        return jdbcTemplate.query("""
                SELECT b.BookingID,m.MovieID,m.Title,m.PosterURL,b.BookingCode,st.StartTime,r.Rating,r.Content,
                       a.FullName CustomerName,a.Email CustomerEmail
                FROM dbo.Booking b JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Account a ON a.AccountID=c.AccountID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID JOIN dbo.Movie m ON m.MovieID=st.MovieID
                LEFT JOIN dbo.MovieReview r ON r.BookingID=b.BookingID AND UPPER(r.[Status])='ACTIVE'
                WHERE c.AccountID=? AND UPPER(b.[Status])='CONFIRMED'
                  AND DATEADD(MINUTE,m.Duration,st.StartTime)<=SYSDATETIME()
                  AND EXISTS(SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PAID')
                ORDER BY st.StartTime DESC
                """, (rs,n) -> new ReviewableMovie(rs.getInt("BookingID"),rs.getInt("MovieID"),rs.getString("Title"),
                rs.getString("PosterURL"),rs.getString("BookingCode"),rs.getObject("StartTime",java.time.LocalDateTime.class),
                (Integer)rs.getObject("Rating"),rs.getString("Content"),rs.getString("CustomerName"),rs.getString("CustomerEmail")), accountId);
    }

    public void saveOrUpdateReview(int customerId, int movieId, int bookingId, int rating, String content) {
        jdbcTemplate.update("""
                MERGE dbo.MovieReview WITH (HOLDLOCK) AS target
                USING (SELECT ? CustomerID,? MovieID,? BookingID,? Rating,? Content) AS source
                ON target.BookingID=source.BookingID
                WHEN MATCHED AND target.CustomerID=source.CustomerID AND target.MovieID=source.MovieID THEN
                    UPDATE SET Rating=source.Rating,Content=source.Content,CreatedAt=SYSDATETIME(),[Status]='ACTIVE'
                WHEN NOT MATCHED THEN
                    INSERT(CustomerID,MovieID,BookingID,Rating,Content,[Status],CreatedAt)
                    VALUES(source.CustomerID,source.MovieID,source.BookingID,source.Rating,source.Content,'ACTIVE',SYSDATETIME());
                """, customerId, movieId, bookingId, rating, content);
    }

    public boolean reportReview(int reviewId, int movieId, int reporterAccountId, String reason, String details) {
        ensureReportTable();
        Integer allowed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM dbo.MovieReview r
                JOIN dbo.Customer author ON author.CustomerID=r.CustomerID
                WHERE r.ReviewID=? AND r.MovieID=? AND UPPER(r.[Status])='ACTIVE'
                  AND author.AccountID<>?
                """, Integer.class, reviewId, movieId, reporterAccountId);
        if (allowed == null || allowed == 0) return false;
        try {
            jdbcTemplate.update("""
                    INSERT dbo.MovieReviewReport(ReviewID,ReporterAccountID,Reason,Details,[Status],CreatedAt)
                    VALUES(?,?,?,?,'PENDING',SYSDATETIME())
                    """, reviewId, reporterAccountId, reason, details);
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    private void ensureReportTable() {
        jdbcTemplate.execute("""
                IF OBJECT_ID(N'dbo.MovieReviewReport', N'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.MovieReviewReport(
                        ReportID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MovieReviewReport PRIMARY KEY,
                        ReviewID INT NOT NULL,
                        ReporterAccountID INT NOT NULL,
                        Reason VARCHAR(40) NOT NULL,
                        Details NVARCHAR(500) NULL,
                        [Status] VARCHAR(20) NOT NULL CONSTRAINT DF_MovieReviewReport_Status DEFAULT 'PENDING',
                        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieReviewReport_CreatedAt DEFAULT SYSDATETIME(),
                        ResolvedAt DATETIME2 NULL,
                        ResolvedByAccountID INT NULL,
                        CONSTRAINT FK_MovieReviewReport_Review FOREIGN KEY(ReviewID) REFERENCES dbo.MovieReview(ReviewID),
                        CONSTRAINT FK_MovieReviewReport_Reporter FOREIGN KEY(ReporterAccountID) REFERENCES dbo.Account(AccountID),
                        CONSTRAINT UQ_MovieReviewReport_Review_Reporter UNIQUE(ReviewID,ReporterAccountID)
                    )
                END
                """);
    }
}
