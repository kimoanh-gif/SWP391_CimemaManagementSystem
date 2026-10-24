package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.exception.RoomHasActiveShowtimesException;
import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for Room Management (Screen 56).
 * Enforces BR-08 (active rooms for showtimes) and BR-52 (cannot deactivate rooms with upcoming showtimes).
 */
@Service
public class RoomService {

    private final RoomDao roomDao;

    public RoomService(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    public List<Room> getAll(String keyword, String status) {
        return roomDao.findAll(keyword, status);
    }

    public Room getById(int id) {
        return roomDao.findById(id).orElseThrow(() -> new RoomNotFoundException(id));
    }

    @Transactional
    public void create(RoomForm form) {
        validateRoomForm(form, null);
        roomDao.insert(form);
    }

    @Transactional
    public void update(RoomForm form) {
        // Ensure room exists
        getById(form.getRoomId());
        validateRoomForm(form, form.getRoomId());
        roomDao.update(form);
    }

    private void validateRoomForm(RoomForm form, Integer currentRoomId) {
        if (form.getRoomCode() != null && roomDao.existsByRoomCode(form.getRoomCode(), currentRoomId)) {
            throw new IllegalArgumentException("Mã phòng chiếu '" + form.getRoomCode().trim() + "' đã tồn tại trong hệ thống. Vui lòng chọn mã khác.");
        }
        if (form.getCapacity() == null || form.getCapacity() <= 0) {
            throw new IllegalArgumentException("Sức chứa phòng phải lớn hơn 0.");
        }
        if (form.getTotalRows() != null && form.getTotalRows() > 0 
                && form.getSeatsPerRow() != null && form.getSeatsPerRow() > 0) {
            int maxSeats = form.getTotalRows() * form.getSeatsPerRow();
            if (form.getCapacity() > maxSeats) {
                throw new IllegalArgumentException("Sức chứa phòng (" + form.getCapacity() 
                        + " ghế) không được vượt quá số ghế tối đa theo ma trận " 
                        + form.getTotalRows() + " hàng x " + form.getSeatsPerRow() + " ghế/hàng (" + maxSeats + " ghế).");
            }
        }
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
     * Room status must be Ready/Screening AND have at least 1 active usable seat (P1-01).
     */
    public boolean isRoomReadyForShowtime(int roomId) {
        Room room = getById(roomId);
        return room.isActiveForShowtimes() && room.getActiveSeatCount() > 0;
    }
}
