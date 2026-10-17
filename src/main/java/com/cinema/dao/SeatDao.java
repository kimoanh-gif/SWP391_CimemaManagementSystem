package com.cinema.dao;

import com.cinema.model.Seat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO for Seat entity.
 * Supports Screen 58: Seat Management / Seat Layout Management (UC-29).
 */
@Repository
public class SeatDao {

    private final JdbcTemplate jdbcTemplate;

    public SeatDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Seat> rowMapper = (rs, rowNum) -> {
        Seat seat = new Seat();
        seat.setSeatId(rs.getInt("SeatID"));
        seat.setRoomId(rs.getInt("RoomID"));
        seat.setSeatTypeName(rs.getString("SeatTypeName"));
        seat.setRowLabel(rs.getString("RowLabel"));
        seat.setSeatNumber(rs.getInt("SeatNumber"));
        seat.setGridRow(rs.getInt("GridRow"));
        seat.setGridColumn(rs.getInt("GridColumn"));
        seat.setStatus(rs.getString("Status"));
        if (rs.getTimestamp("CreatedAt") != null) {
            seat.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
        }
        return seat;
    };

    public List<Seat> findByRoomId(int roomId) {
        String sql = "SELECT * FROM Seat WHERE RoomID = ? ORDER BY GridRow ASC, GridColumn ASC";
        return jdbcTemplate.query(sql, ps -> ps.setInt(1, roomId), rowMapper);
    }

    public Optional<Seat> findById(int seatId) {
        String sql = "SELECT * FROM Seat WHERE SeatID = ?";
        List<Seat> list = jdbcTemplate.query(sql, ps -> ps.setInt(1, seatId), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Seat> findByRoomAndPosition(int roomId, String rowLabel, int seatNumber) {
        String sql = "SELECT * FROM Seat WHERE RoomID = ? AND RowLabel = ? AND SeatNumber = ?";
        List<Seat> list = jdbcTemplate.query(sql, ps -> {
            ps.setInt(1, roomId);
            ps.setString(2, rowLabel);
            ps.setInt(3, seatNumber);
        }, rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int countByRoomId(int roomId) {
        String sql = "SELECT COUNT(*) FROM Seat WHERE RoomID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roomId);
        return count != null ? count : 0;
    }

    public int insert(Seat seat) {
        String sql = "INSERT INTO Seat (RoomID, SeatTypeName, RowLabel, SeatNumber, GridRow, GridColumn, Status, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME())";
        return jdbcTemplate.update(sql,
                seat.getRoomId(),
                seat.getSeatTypeName() != null ? seat.getSeatTypeName() : "Standard",
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getGridRow(),
                seat.getGridColumn(),
                seat.getStatus() != null ? seat.getStatus() : "Active"
        );
    }

    public int update(Seat seat) {
        String sql = "UPDATE Seat SET SeatTypeName = ?, RowLabel = ?, SeatNumber = ?, GridRow = ?, GridColumn = ?, Status = ? " +
                "WHERE SeatID = ?";
        return jdbcTemplate.update(sql,
                seat.getSeatTypeName(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getGridRow(),
                seat.getGridColumn(),
                seat.getStatus(),
                seat.getSeatId()
        );
    }

    public int updateSeatType(int seatId, String seatTypeName) {
        String sql = "UPDATE Seat SET SeatTypeName = ? WHERE SeatID = ?";
        return jdbcTemplate.update(sql, seatTypeName, seatId);
    }

    public int updateStatus(int seatId, String status) {
        String sql = "UPDATE Seat SET Status = ? WHERE SeatID = ?";
        return jdbcTemplate.update(sql, status, seatId);
    }

    public int deleteByRoomId(int roomId) {
        String sql = "DELETE FROM Seat WHERE RoomID = ?";
        return jdbcTemplate.update(sql, roomId);
    }

    public int deleteById(int seatId) {
        String sql = "DELETE FROM Seat WHERE SeatID = ?";
        return jdbcTemplate.update(sql, seatId);
    }

    public boolean hasFutureBookings(int roomId) {
        String sql = "SELECT COUNT(*) FROM Showtime sh WHERE sh.RoomID = ? AND sh.StartTime > GETDATE()";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roomId);
        return count != null && count > 0;
    }

    /**
     * Checks whether a seat is referenced in historical or active tickets/bookings (AC02).
     */
    public boolean isSeatReferenced(int seatId) {
        // 1. Check Ticket table if it exists
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Ticket WHERE SeatID = ?", Integer.class, seatId);
            if (count != null && count > 0) return true;
        } catch (Exception ignored) {}

        // 2. Check BookingDetail table if it exists
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BookingDetail WHERE SeatID = ?", Integer.class, seatId);
            if (count != null && count > 0) return true;
        } catch (Exception ignored) {}

        // 3. Check BookingSeat table if it exists
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BookingSeat WHERE SeatID = ?", Integer.class, seatId);
            if (count != null && count > 0) return true;
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Checks whether the room has any upcoming showtimes with active bookings.
     */
    public boolean hasUpcomingBookingsForRoom(int roomId) {
        try {
            String sql = """
                SELECT COUNT(*)
                FROM Booking b
                JOIN Showtime s ON b.ShowtimeID = s.ShowtimeID
                WHERE s.RoomID = ?
                  AND s.StartTime >= SYSUTCDATETIME()
                  AND s.Status NOT IN ('Cancelled', 'Stopped')
                  AND b.Status IN ('Pending', 'Paid', 'Confirmed', 'Held')
            """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roomId);
            return count != null && count > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Checks whether a specific seat is booked in upcoming showtimes for this room.
     */
    public boolean isSeatBookedInUpcomingShowtimes(int seatId, int roomId) {
        try {
            String sql = """
                SELECT COUNT(*)
                FROM Ticket t
                JOIN Showtime s ON t.ShowtimeID = s.ShowtimeID
                WHERE t.SeatID = ?
                  AND s.RoomID = ?
                  AND s.StartTime >= SYSUTCDATETIME()
                  AND s.Status NOT IN ('Cancelled', 'Stopped')
            """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, seatId, roomId);
            return count != null && count > 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}

