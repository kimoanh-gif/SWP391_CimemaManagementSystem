package com.cinema.dao;

import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Room entity. Implements CRUD operations and queries needed by Screen 56.
 */
@Repository
public class RoomDao {

    private final JdbcTemplate jdbcTemplate;

    public RoomDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Room> roomRowMapper = (rs, rowNum) -> {
        Room room = new Room();
        room.setRoomId(rs.getInt("RoomID"));
        room.setRoomCode(rs.getString("RoomCode"));
        room.setRoomName(rs.getString("RoomName"));
        room.setRoomType(rs.getString("RoomType"));
        room.setCapacity(rs.getInt("Capacity"));
        room.setTotalRows(rs.getObject("TotalRows", Integer.class));
        room.setSeatsPerRow(rs.getObject("SeatsPerRow", Integer.class));
        room.setProjector(rs.getString("Projector"));
        room.setSoundSystem(rs.getString("SoundSystem"));
        room.setScreenSize(rs.getString("ScreenSize"));
        room.setStatus(rs.getString("Status"));
        room.setDescription(rs.getString("Description"));
        java.sql.Timestamp createdAt = rs.getTimestamp("CreatedAt");
        java.sql.Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        room.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        room.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        room.setSeatCount(rs.getInt("SeatCount"));
        try {
            room.setActiveSeatCount(rs.getInt("ActiveSeatCount"));
        } catch (Exception ignored) {
            room.setActiveSeatCount(rs.getInt("SeatCount"));
        }
        room.setShowtimeCount(rs.getInt("ShowtimeCount"));
        return room;
    };

    public List<Room> findAll(String keyword, String status) {
        String sql = "SELECT r.*, " +
                "(SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID) AS SeatCount, " +
                "(SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID AND s.Status = 'Active') AS ActiveSeatCount, " +
                "(SELECT COUNT(*) FROM Showtime sh WHERE sh.RoomID = r.RoomID) AS ShowtimeCount " +
                "FROM Room r " +
                "WHERE ( ? IS NULL OR r.RoomName LIKE ? OR r.RoomCode LIKE ? ) " +
                "AND ( ? IS NULL OR r.Status = ? ) " +
                "ORDER BY r.RoomID DESC";
        String kwPattern = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%";
        return jdbcTemplate.query(sql,
                ps -> {
                    ps.setString(1, kwPattern);
                    ps.setString(2, kwPattern);
                    ps.setString(3, kwPattern);
                    ps.setString(4, status);
                    ps.setString(5, status);
                },
                roomRowMapper);
    }

    public Optional<Room> findById(int roomId) {
        String sql = "SELECT r.*, " +
                "(SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID) AS SeatCount, " +
                "(SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID AND s.Status = 'Active') AS ActiveSeatCount, " +
                "(SELECT COUNT(*) FROM Showtime sh WHERE sh.RoomID = r.RoomID) AS ShowtimeCount " +
                "FROM Room r WHERE r.RoomID = ?";
        List<Room> list = jdbcTemplate.query(sql, roomRowMapper, roomId);
        return list.stream().findFirst();
    }

    public int insert(RoomForm form) {
        String sql = "INSERT INTO Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, " +
                "Projector, SoundSystem, ScreenSize, Status, Description, CreatedAt, UpdatedAt) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return jdbcTemplate.update(sql,
                form.getRoomCode(),
                form.getRoomName(),
                form.getRoomType(),
                form.getCapacity(),
                form.getTotalRows(),
                form.getSeatsPerRow(),
                form.getProjector(),
                form.getSoundSystem(),
                form.getScreenSize(),
                form.getStatus(),
                form.getDescription(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());
    }

    public int update(RoomForm form) {
        String sql = "UPDATE Room SET RoomCode = ?, RoomName = ?, RoomType = ?, Capacity = ?, " +
                "TotalRows = ?, SeatsPerRow = ?, Projector = ?, SoundSystem = ?, ScreenSize = ?, " +
                "Status = ?, Description = ?, UpdatedAt = ? WHERE RoomID = ?";
        return jdbcTemplate.update(sql,
                form.getRoomCode(),
                form.getRoomName(),
                form.getRoomType(),
                form.getCapacity(),
                form.getTotalRows(),
                form.getSeatsPerRow(),
                form.getProjector(),
                form.getSoundSystem(),
                form.getScreenSize(),
                form.getStatus(),
                form.getDescription(),
                java.time.LocalDateTime.now(),
                form.getRoomId());
    }

    public int delete(int roomId) {
        String sql = "DELETE FROM Room WHERE RoomID = ?";
        return jdbcTemplate.update(sql, roomId);
    }

    public int countTotalShowtimes(int roomId) {
        String sql = "SELECT COUNT(*) FROM Showtime WHERE RoomID = ?";
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, roomId);
        return cnt != null ? cnt : 0;
    }

    public void deleteSeatsByRoomId(int roomId) {
        jdbcTemplate.update("DELETE FROM Seat WHERE RoomID = ?", roomId);
    }

    /**
     * Checks whether a room has any upcoming (future) showtimes.
     * Used for BR-52 validation before deactivation.
     */
    public boolean hasUpcomingShowtimes(int roomId) {
        String sql = "SELECT COUNT(*) FROM Showtime sh WHERE sh.RoomID = ? AND sh.StartTime > GETDATE()";
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, roomId);
        return cnt != null && cnt > 0;
    }

    /**
     * Locks the Room record within the current transaction using UPDLOCK, HOLDLOCK.
     * Serializes concurrent modifications (Showtime creation, Seat layout updates) per Room.
     */
    public void lockRoomForUpdate(int roomId) {
        String sql = "SELECT RoomID FROM Room WITH (UPDLOCK, HOLDLOCK) WHERE RoomID = ?";
        jdbcTemplate.queryForObject(sql, Integer.class, roomId);
    }

    /**
     * Updates the UpdatedAt timestamp on the Room record to track layout/room updates.
     */
    public void updateRoomTimestamp(int roomId) {
        String sql = "UPDATE Room SET UpdatedAt = SYSUTCDATETIME() WHERE RoomID = ?";
        jdbcTemplate.update(sql, roomId);
    }
}

