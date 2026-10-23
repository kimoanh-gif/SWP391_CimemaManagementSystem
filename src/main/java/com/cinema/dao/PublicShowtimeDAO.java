package com.cinema.dao;

import com.cinema.model.PublicShowtime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Read-only showtime queries for the public screens (Home Page, Movie Details, Showtime List).
 *
 * Business rules (SRS UC03):
 * - BR-ST-02: only Scheduled / Active showtimes that have not started yet are returned.
 * - BR-ST-03: the movie must be public (Now Showing / Upcoming) and the room must be operational.
 * - BR-ST-07 / BR-ST-08: movie, genre, age rating and room type filters are combined with AND.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Repository
public class PublicShowtimeDAO {

    /** Conditions every public showtime must satisfy. */
    private static final String PUBLIC_SHOWTIME_CONDITION = """
              st.StartTime > SYSDATETIME()
              AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
              AND UPPER(r.[Status]) IN ('ACTIVE', 'READY', 'SCREENING')
              AND UPPER(m.[Status]) IN ('NOW SHOWING', 'NOW_SHOWING', 'UPCOMING', 'COMING SOON', 'COMING_SOON')
            """;

    private static final String BASE_SHOWTIME_QUERY = """
            SELECT st.ShowtimeID, st.MovieID, st.RoomID, st.StartTime, st.EndTime,
                   st.BasePrice, st.[Status],
                   m.Title, m.PosterURL, m.AgeRating, m.Duration,
                   (SELECT STRING_AGG(g.[Name], ', ')
                    FROM dbo.MovieGenre mg
                    JOIN dbo.Genre g ON g.GenreID = mg.GenreID
                    WHERE mg.MovieID = m.MovieID) AS Genres,
                   r.RoomName, r.RoomType,
                   r.Capacity AS TotalSeats,
                   (SELECT COUNT(*)
                    FROM dbo.Seat seat
                    WHERE seat.RoomID = st.RoomID
                      AND UPPER(seat.[Status]) = 'ACTIVE'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM dbo.BookingSeat bs
                          JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                          WHERE bs.ShowtimeID = st.ShowtimeID
                            AND bs.SeatID = seat.SeatID
                            AND UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                            AND (UPPER(bs.[Status]) IN ('BOOKED', 'CONFIRMED')
                                 OR (UPPER(bs.[Status]) = 'HELD' AND bs.HeldUntil > SYSDATETIME()))
                      )) AS AvailableSeats
            FROM dbo.Showtime st
            JOIN dbo.Movie m ON m.MovieID = st.MovieID
            JOIN dbo.Room r ON r.RoomID = st.RoomID
            """;

    private final JdbcTemplate jdbcTemplate;

    public PublicShowtimeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PublicShowtime> rowMapper = (rs, rowNum) -> mapPublicShowtime(rs);

    private PublicShowtime mapPublicShowtime(ResultSet rs) throws SQLException {
        PublicShowtime st = new PublicShowtime();
        st.setShowtimeId(rs.getInt("ShowtimeID"));
        st.setMovieId(rs.getInt("MovieID"));
        st.setMovieTitle(rs.getString("Title"));
        st.setMoviePosterUrl(rs.getString("PosterURL"));
        st.setMovieAgeRating(rs.getString("AgeRating"));
        st.setMovieDuration(rs.getInt("Duration"));
        st.setMovieGenres(rs.getString("Genres"));

        st.setRoomId(rs.getInt("RoomID"));
        st.setRoomName(rs.getString("RoomName"));
        st.setRoomType(rs.getString("RoomType"));

        Timestamp start = rs.getTimestamp("StartTime");
        st.setStartTime(start != null ? start.toLocalDateTime() : null);

        Timestamp end = rs.getTimestamp("EndTime");
        st.setEndTime(end != null ? end.toLocalDateTime() : null);

        st.setBasePrice(rs.getBigDecimal("BasePrice"));
        st.setStatus(rs.getString("Status"));
        st.setAvailableSeats(rs.getInt("AvailableSeats"));
        st.setTotalSeats(rs.getInt("TotalSeats"));

        return st;
    }

    /**
     * Showtimes for one date with every optional filter applied (BR-ST-08).
     *
     * @param timeSlot "morning" (before 12:00), "afternoon" (12:00–18:00), "evening" (after 18:00)
     */
    public List<PublicShowtime> findShowtimes(LocalDate date, Integer movieId, Integer genreId,
                                              String ageRating, String roomType, String timeSlot) {
        StringBuilder sql = new StringBuilder(BASE_SHOWTIME_QUERY);
        sql.append(" WHERE CAST(st.StartTime AS DATE) = ? AND ").append(PUBLIC_SHOWTIME_CONDITION);

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(date));

        if (movieId != null && movieId > 0) {
            sql.append(" AND st.MovieID = ? ");
            params.add(movieId);
        }

        if (genreId != null && genreId > 0) {
            sql.append(" AND EXISTS (SELECT 1 FROM dbo.MovieGenre fg WHERE fg.MovieID = m.MovieID AND fg.GenreID = ?) ");
            params.add(genreId);
        }

        if (ageRating != null && !ageRating.isBlank()) {
            sql.append(" AND UPPER(m.AgeRating) = ? ");
            params.add(ageRating.trim().toUpperCase());
        }

        if (roomType != null && !roomType.isBlank() && !"ALL".equalsIgnoreCase(roomType)) {
            sql.append(" AND UPPER(r.RoomType) = ? ");
            params.add(roomType.trim().toUpperCase());
        }

        if ("morning".equalsIgnoreCase(timeSlot)) {
            sql.append(" AND DATEPART(HOUR, st.StartTime) < 12 ");
        } else if ("afternoon".equalsIgnoreCase(timeSlot)) {
            sql.append(" AND DATEPART(HOUR, st.StartTime) >= 12 AND DATEPART(HOUR, st.StartTime) < 18 ");
        } else if ("evening".equalsIgnoreCase(timeSlot)) {
            sql.append(" AND DATEPART(HOUR, st.StartTime) >= 18 ");
        }

        sql.append(" ORDER BY m.Title ASC, st.StartTime ASC ");
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<PublicShowtime> findShowtimesByMovieAndDate(int movieId, LocalDate date) {
        String sql = BASE_SHOWTIME_QUERY
                + " WHERE st.MovieID = ? AND CAST(st.StartTime AS DATE) = ? AND " + PUBLIC_SHOWTIME_CONDITION
                + " ORDER BY st.StartTime ASC";
        return jdbcTemplate.query(sql, rowMapper, movieId, Date.valueOf(date));
    }

    public List<LocalDate> findAvailableDatesForMovie(int movieId) {
        String sql = """
                SELECT DISTINCT CAST(st.StartTime AS DATE) AS ShowDate
                FROM dbo.Showtime st
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                WHERE st.MovieID = ? AND
                """ + PUBLIC_SHOWTIME_CONDITION + " ORDER BY ShowDate ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date d = rs.getDate("ShowDate");
            return d != null ? d.toLocalDate() : null;
        }, movieId);
    }

    public List<PublicShowtime> findShowtimesForToday(int limit) {
        String sql = BASE_SHOWTIME_QUERY
                + " WHERE CAST(st.StartTime AS DATE) = CAST(SYSDATETIME() AS DATE) AND " + PUBLIC_SHOWTIME_CONDITION
                + " ORDER BY st.StartTime ASC";
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        return jdbcTemplate.query(sql, rowMapper);
    }

    /** Distinct room types of operational rooms, used by the Room Type filter (BR-ST-07). */
    public List<String> findActiveRoomTypes() {
        String sql = """
                SELECT DISTINCT r.RoomType
                FROM dbo.Room r
                WHERE UPPER(r.[Status]) IN ('ACTIVE', 'READY', 'SCREENING')
                  AND r.RoomType IS NOT NULL
                ORDER BY r.RoomType
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("RoomType"));
    }

    public Optional<PublicShowtime> findById(int showtimeId) {
        String sql = BASE_SHOWTIME_QUERY + " WHERE st.ShowtimeID = ?";
        try {
            PublicShowtime st = jdbcTemplate.queryForObject(sql, rowMapper, showtimeId);
            return Optional.ofNullable(st);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
