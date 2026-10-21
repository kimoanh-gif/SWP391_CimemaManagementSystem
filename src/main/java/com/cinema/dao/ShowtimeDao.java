package com.cinema.dao;

import com.cinema.model.Showtime;
import com.cinema.model.ShowtimeForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Showtime table (Screen 59 / UC-30).
 */
@Repository
public class ShowtimeDao {

    private final JdbcTemplate jdbcTemplate;

    private volatile boolean columnsChecked = false;
    private String basePriceColumn = "BasePrice";

    public ShowtimeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private synchronized void checkColumns() {
        if (columnsChecked) {
            return;
        }
        try {
            Integer isBasePriceComputed = jdbcTemplate.queryForObject(
                    "SELECT COLUMNPROPERTY(OBJECT_ID('dbo.Showtime'), 'BasePrice', 'IsComputed')", Integer.class);
            if (isBasePriceComputed != null && isBasePriceComputed == 1) {
                basePriceColumn = "BaseTicketPrice";
            }
        } catch (Exception ex) {
            // Keep default if check fails
        }
        columnsChecked = true;
    }

    private final RowMapper<Showtime> rowMapper = (rs, rowNum) -> {
        Showtime st = new Showtime();
        st.setShowtimeId(rs.getInt("ShowtimeID"));
        st.setMovieId(rs.getInt("MovieID"));
        st.setRoomId(rs.getObject("RoomID", Integer.class));
        st.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
        st.setEndTime(rs.getTimestamp("EndTime").toLocalDateTime());
        st.setBaseTicketPrice(rs.getBigDecimal("BaseTicketPrice"));
        st.setStatus(rs.getString("Status"));
        st.setCreatedAt(rs.getTimestamp("CreatedAt") != null ? rs.getTimestamp("CreatedAt").toLocalDateTime() : null);

        // Joined Movie fields
        st.setMovieTitle(rs.getString("MovieTitle"));
        st.setMoviePoster(rs.getString("MoviePoster"));
        st.setMovieDuration(rs.getObject("MovieDuration", Integer.class));
        st.setMovieAgeRating(rs.getString("MovieAgeRating"));

        // Joined Room fields
        st.setRoomName(rs.getString("RoomName"));
        st.setRoomCode(rs.getString("RoomCode"));
        st.setRoomType(rs.getString("RoomType"));
        st.setRoomCapacity(rs.getObject("RoomCapacity", Integer.class));

        return st;
    };

    public List<Showtime> findAll(LocalDate date, Integer roomId, Integer movieId, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, s.BasePrice AS BaseTicketPrice, " +
                "m.Title AS MovieTitle, m.PosterURL AS MoviePoster, m.Duration AS MovieDuration, m.AgeRating AS MovieAgeRating, " +
                "r.RoomName, r.RoomCode, r.RoomType, r.Capacity AS RoomCapacity " +
                "FROM Showtime s " +
                "JOIN Movie m ON s.MovieID = m.MovieID " +
                "LEFT JOIN Room r ON s.RoomID = r.RoomID " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (date != null) {
            sql.append("AND CAST(s.StartTime AS DATE) = ? ");
            params.add(date);
        }
        if (roomId != null && roomId > 0) {
            sql.append("AND s.RoomID = ? ");
            params.add(roomId);
        }
        if (movieId != null && movieId > 0) {
            sql.append("AND s.MovieID = ? ");
            params.add(movieId);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND s.Status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY s.StartTime ASC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), rowMapper);
    }

    public Optional<Showtime> findById(int id) {
        String sql = "SELECT s.*, s.BasePrice AS BaseTicketPrice, " +
                "m.Title AS MovieTitle, m.PosterURL AS MoviePoster, m.Duration AS MovieDuration, m.AgeRating AS MovieAgeRating, " +
                "r.RoomName, r.RoomCode, r.RoomType, r.Capacity AS RoomCapacity " +
                "FROM Showtime s " +
                "JOIN Movie m ON s.MovieID = m.MovieID " +
                "LEFT JOIN Room r ON s.RoomID = r.RoomID " +
                "WHERE s.ShowtimeID = ?";
        List<Showtime> list = jdbcTemplate.query(sql, ps -> ps.setInt(1, id), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Checks if there are overlapping showtimes in the same room.
     * Overlap rule per SRS (BR-09): NextStart >= PreviousEnd + 15 minutes.
     * Symmetrical conflict condition:
     *   newStart < DATEADD(minute, 15, s.EndTime) AND s.StartTime < DATEADD(minute, 15, newEnd)
     * Exclude current record if updating (excludeShowtimeId != null).
     * Exclude cancelled or stopped showtimes.
     */
    public List<Showtime> findOverlappingShowtimes(int roomId, LocalDateTime start, LocalDateTime end, Integer excludeShowtimeId) {
        String sql = "SELECT s.*, s.BasePrice AS BaseTicketPrice, " +
                "m.Title AS MovieTitle, m.PosterURL AS MoviePoster, m.Duration AS MovieDuration, m.AgeRating AS MovieAgeRating, " +
                "r.RoomName, r.RoomCode, r.RoomType, r.Capacity AS RoomCapacity " +
                "FROM Showtime s " +
                "JOIN Movie m ON s.MovieID = m.MovieID " +
                "LEFT JOIN Room r ON s.RoomID = r.RoomID " +
                "WHERE s.RoomID = ? " +
                "AND s.Status NOT IN ('Cancelled', 'Stopped') " +
                "AND (? < DATEADD(minute, 15, s.EndTime) AND s.StartTime < DATEADD(minute, 15, ?)) " +
                (excludeShowtimeId != null ? "AND s.ShowtimeID <> ? " : "");

        List<Object> params = new ArrayList<>();
        params.add(roomId);
        params.add(Timestamp.valueOf(start));
        params.add(Timestamp.valueOf(end));
        if (excludeShowtimeId != null) {
            params.add(excludeShowtimeId);
        }

        return jdbcTemplate.query(sql, params.toArray(), rowMapper);
    }


    public int insert(ShowtimeForm form) {
        checkColumns();
        String sql = "INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, " + basePriceColumn + ", Status, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, SYSUTCDATETIME())";
        return jdbcTemplate.update(sql,
                form.getMovieId(),
                form.getRoomId(),
                Timestamp.valueOf(form.getStartTime()),
                Timestamp.valueOf(form.getEndTime()),
                form.getBaseTicketPrice(),
                form.getStatus() != null ? form.getStatus() : "Scheduled"
        );
    }

    public int update(int id, ShowtimeForm form) {
        checkColumns();
        String sql = "UPDATE Showtime SET MovieID = ?, RoomID = ?, StartTime = ?, EndTime = ?, " +
                basePriceColumn + " = ?, Status = ? WHERE ShowtimeID = ?";
        return jdbcTemplate.update(sql,
                form.getMovieId(),
                form.getRoomId(),
                Timestamp.valueOf(form.getStartTime()),
                Timestamp.valueOf(form.getEndTime()),
                form.getBaseTicketPrice(),
                form.getStatus(),
                id
        );
    }

    public int updateStatus(int id, String status) {
        String sql = "UPDATE Showtime SET Status = ? WHERE ShowtimeID = ?";
        return jdbcTemplate.update(sql, status, id);
    }

    /**
     * Checks if this showtime has active or paid bookings (BR-54).
     */
    public boolean hasActiveBookings(int showtimeId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Booking WHERE ShowtimeID = ? AND Status IN ('Pending', 'Paid', 'Confirmed')",
                    Integer.class, showtimeId);
            return count != null && count > 0;
        } catch (Exception ex) {
            // If Booking table is not created yet, return false
            return false;
        }
    }

    /**
     * Checks if this showtime has any bookings or booked seats (BR-54 and FK protection).
     */
    public boolean hasAnyBookings(int showtimeId) {
        try {
            Integer countBooking = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Booking WHERE ShowtimeID = ?",
                    Integer.class, showtimeId);
            if (countBooking != null && countBooking > 0) {
                return true;
            }

            Integer countSeats = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BookingSeat WHERE ShowtimeID = ? AND Status = 'BOOKED'",
                    Integer.class, showtimeId);
            return countSeats != null && countSeats > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Safely deletes a showtime.
     */
    public int delete(int id) {
        try {
            jdbcTemplate.update("DELETE FROM BookingSeat WHERE ShowtimeID = ?", id);
        } catch (Exception ignored) {}
        String sql = "DELETE FROM Showtime WHERE ShowtimeID = ?";
        return jdbcTemplate.update(sql, id);
    }
}
