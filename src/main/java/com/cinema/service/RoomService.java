package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.exception.RoomHasActiveShowtimesException;
import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Business logic for Room Management (Screen 56).
 * Enforces BR-08 (active rooms for showtimes) and BR-52 (cannot deactivate
 * rooms with upcoming showtimes).
 */
@Service
public class RoomService {

    private static final Set<String> VALID_STATUSES = Set.of("Ready", "Screening", "Cleaning", "Maintenance");

    private final RoomDao roomDao;
    private final SeatDao seatDao;

    public RoomService(RoomDao roomDao, SeatDao seatDao) {
        this.roomDao = roomDao;
        this.seatDao = seatDao;
    }

    public List<Room> getAll(String keyword, String status) {
        return roomDao.findAll(keyword, status);
    }

    public Room getById(int id) {
        return roomDao.findById(id).orElseThrow(() -> new RoomNotFoundException(id));
    }

    public void validateCapacityAndLayout(RoomForm form) {
        if (form == null)
            return;
        if (form.getCapacity() == null || form.getCapacity() <= 0) {
            throw new IllegalArgumentException(
                    "Please enter total room capacity greater than 0.");
        }
        boolean hasRows = form.getTotalRows() != null && form.getTotalRows() > 0;
        boolean hasSeatsPerRow = form.getSeatsPerRow() != null && form.getSeatsPerRow() > 0;
        if (!hasRows || !hasSeatsPerRow) {
            throw new IllegalArgumentException(
                    "Please enter vertical rows and horizontal seats greater than 0.");
        }
        int expected = form.getTotalRows() * form.getSeatsPerRow();
        if (!form.getCapacity().equals(expected)) {
            throw new IllegalArgumentException(
                    "Total capacity (" + form.getCapacity() + " seats) does not match rows ("
                            + form.getTotalRows() + ") × seats per row ("
                            + form.getSeatsPerRow() + " = " + expected
                            + " seats). Total room capacity must equal vertical rows multiplied by horizontal seats.");
        }
    }

    private void validateUniquenessAndStatus(RoomForm form, Integer excludeRoomId) {
        if (form == null)
            return;
        if (form.getRoomCode() != null && !form.getRoomCode().isBlank()) {
            form.setRoomCode(form.getRoomCode().trim());
            if (roomDao.existsByRoomCode(form.getRoomCode(), excludeRoomId)) {
                throw new IllegalArgumentException(
                        "Room code already exists in the system. Please enter a different room code.");
            }
        }
        if (form.getRoomName() != null && !form.getRoomName().isBlank()) {
            form.setRoomName(form.getRoomName().trim());
            if (roomDao.existsByRoomName(form.getRoomName(), excludeRoomId)) {
                throw new IllegalArgumentException(
                        "Room name already exists in the system. Please enter a different room name.");
            }
        }
        if (form.getStatus() != null && !form.getStatus().isBlank()) {
            String trimmedStatus = form.getStatus().trim();
            if (!VALID_STATUSES.contains(trimmedStatus)) {
                throw new IllegalArgumentException("Invalid room status: " + trimmedStatus);
            }
            form.setStatus(trimmedStatus);
        }
    }

    @Transactional
    public void create(RoomForm form) {
        validateCapacityAndLayout(form);
        validateUniquenessAndStatus(form, null);
        roomDao.insert(form);
    }

    @Transactional
    public void update(RoomForm form) {
        Room existing = getById(form.getRoomId());
        validateCapacityAndLayout(form);
        validateUniquenessAndStatus(form, form.getRoomId());

        if ("Maintenance".equalsIgnoreCase(form.getStatus())
                && (existing.getStatus() == null || !"Maintenance".equalsIgnoreCase(existing.getStatus()))
                && roomDao.hasUpcomingShowtimes(form.getRoomId())) {
            throw new RoomHasActiveShowtimesException(form.getRoomId());
        }

        roomDao.update(form);
    }

    @Transactional
    public void deactivate(int roomId) {
        Room room = getById(roomId);
        // BR-52: cannot deactivate if upcoming showtimes exist
        if (roomDao.hasUpcomingShowtimes(roomId)) {
            throw new RoomHasActiveShowtimesException(roomId);
        }
        // Change status to Maintenance (or a dedicated Deactivated status if defined)
        room.setStatus("Maintenance");
        RoomForm form = new RoomForm();
        form.setRoomId(roomId);
        form.setRoomCode(room.getRoomCode());
        form.setRoomName(room.getRoomName());
        form.setRoomType(room.getRoomType());
        form.setCapacity(room.getCapacity());
        form.setTotalRows(room.getTotalRows());
        form.setSeatsPerRow(room.getSeatsPerRow());
        form.setProjector(room.getProjector());
        form.setSoundSystem(room.getSoundSystem());
        form.setScreenSize(room.getScreenSize());
        form.setStatus("Maintenance");
        form.setDescription(room.getDescription());
        roomDao.update(form);
    }

    /**
     * Checks if a room is eligible for showtime scheduling:
     * Room status must be Ready/Screening AND have at least 1 active usable seat
     * (P1-01).
     */
    public boolean isRoomReadyForShowtime(int roomId) {
        Room room = getById(roomId);
        return room.isActiveForShowtimes() && room.getActiveSeatCount() > 0;
    }

    @Transactional
    public void delete(int roomId) {
        Room room = getById(roomId);
        int showtimes = roomDao.countTotalShowtimes(roomId);
        if (showtimes > 0) {
            throw new RoomHasActiveShowtimesException(
                    "Cannot delete screening room '" + room.getRoomName() + "' because it has " + showtimes
                            + " linked showtime(s). Please delete showtimes first or set room to Maintenance.");
        }
        roomDao.deleteSeatsByRoomId(roomId);
        roomDao.delete(roomId);
    }
}
