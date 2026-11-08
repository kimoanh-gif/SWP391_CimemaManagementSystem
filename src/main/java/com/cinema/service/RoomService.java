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
        validateUniquenessAndStatus(form, form.getRoomId());

        boolean hasUpcomingShowtimes = roomDao.hasUpcomingShowtimes(form.getRoomId());

        if ("Maintenance".equalsIgnoreCase(form.getStatus())
                && (existing.getStatus() == null || !"Maintenance".equalsIgnoreCase(existing.getStatus()))
                && hasUpcomingShowtimes) {
            throw new RoomHasActiveShowtimesException(form.getRoomId());
        }

        // Prevent changing room capacity or dimensions if upcoming showtimes are scheduled
        if (hasUpcomingShowtimes) {
            if (!java.util.Objects.equals(existing.getCapacity(), form.getCapacity())
                    || !java.util.Objects.equals(existing.getTotalRows(), form.getTotalRows())
                    || !java.util.Objects.equals(existing.getSeatsPerRow(), form.getSeatsPerRow())) {
                throw new RoomHasActiveShowtimesException("Cannot change room capacity or layout dimensions because the room has active or upcoming showtimes.");
            }
        }

        List<com.cinema.model.Seat> existingSeats = seatDao != null ? seatDao.findByRoomId(form.getRoomId()) : java.util.Collections.emptyList();
        if (existingSeats != null && !existingSeats.isEmpty()) {
            if (form.getCapacity() == null || form.getCapacity() <= 0) {
                throw new IllegalArgumentException("Please enter total room capacity greater than 0.");
            }
            if (form.getTotalRows() == null || form.getTotalRows() <= 0 || form.getSeatsPerRow() == null || form.getSeatsPerRow() <= 0) {
                throw new IllegalArgumentException("Please enter vertical rows and horizontal seats greater than 0.");
            }
            // 1. Capacity must match actual configured physical seat count
            if (!form.getCapacity().equals(existingSeats.size())) {
                throw new IllegalArgumentException(
                        "Room capacity (" + form.getCapacity() + " seats) does not match the configured Seat Map ("
                                + existingSeats.size() + " seats). Please update the Seat Map before changing room capacity.");
            }
            // 2. TotalRows and SeatsPerRow must accommodate all existing seat coordinates
            int maxRow = existingSeats.stream().mapToInt(com.cinema.model.Seat::getGridRow).filter(r -> r > 0).max().orElse(1);
            int maxCol = existingSeats.stream().mapToInt(com.cinema.model.Seat::getGridColumn).filter(c -> c > 0).max().orElse(1);
            if (form.getTotalRows() < maxRow || form.getSeatsPerRow() < maxCol) {
                throw new IllegalArgumentException(
                        "Room dimensions (" + form.getTotalRows() + " rows × " + form.getSeatsPerRow()
                                + " seats per row) cannot be smaller than existing seat coordinates (row "
                                + maxRow + ", column " + maxCol + ").");
            }
            // 3. Room dimensions must accommodate total capacity
            if (form.getCapacity() > form.getTotalRows() * form.getSeatsPerRow()) {
                throw new IllegalArgumentException(
                        "Total capacity (" + form.getCapacity() + " seats) cannot exceed maximum grid positions ("
                                + (form.getTotalRows() * form.getSeatsPerRow()) + " seats).");
            }
        } else {
            // Room has NO configured seats yet: enforce rectangular layout
            validateCapacityAndLayout(form);
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
