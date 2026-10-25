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
import java.util.Optional;

/**
 * Business logic for Room Management (Screen 56).
 * Enforces BR-08 (active rooms for showtimes) and BR-52 (cannot deactivate rooms with upcoming showtimes).
 */
@Service
public class RoomService {

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
        if (form.getTotalRows() == null || form.getTotalRows() <= 0) {
            throw new IllegalArgumentException("Vui lòng nhập tổng số hàng ghế (hàng dọc) lớn hơn 0.");
        }
        if (form.getSeatsPerRow() == null || form.getSeatsPerRow() <= 0) {
            throw new IllegalArgumentException("Vui lòng nhập số ghế mỗi hàng (hàng ngang) lớn hơn 0.");
        }
        if (form.getCapacity() == null || form.getCapacity() <= 0) {
            throw new IllegalArgumentException("Vui lòng nhập tổng số ghế cả phòng lớn hơn 0.");
        }
        int expectedCapacity = form.getTotalRows() * form.getSeatsPerRow();
        if (form.getCapacity() != expectedCapacity) {
            throw new IllegalArgumentException(
                String.format("Tổng số ghế cả phòng (%d) phải bằng tổng số ghế hàng dọc nhân với tổng số ghế hàng ngang (%d hàng × %d ghế = %d ghế).",
                    form.getCapacity(),
                    form.getTotalRows(),
                    form.getSeatsPerRow(),
                    expectedCapacity)
            );
        }
    }

    @Transactional
    public void create(RoomForm form) {
        validateCapacityAndLayout(form);
        if (roomDao.existsByRoomCode(form.getRoomCode(), null)) {
            throw new IllegalArgumentException("Mã phòng chiếu '" + form.getRoomCode().trim() + "' đã tồn tại trong hệ thống. Vui lòng nhập mã phòng khác.");
        }
        if (roomDao.existsByRoomName(form.getRoomName(), null)) {
            throw new IllegalArgumentException("Tên phòng chiếu '" + form.getRoomName().trim() + "' đã tồn tại trong hệ thống. Vui lòng đặt tên phòng khác.");
        }
        roomDao.insert(form);
    }

    @Transactional
    public void update(RoomForm form) {
        getById(form.getRoomId());
        validateCapacityAndLayout(form);
        if (roomDao.existsByRoomCode(form.getRoomCode(), form.getRoomId())) {
            throw new IllegalArgumentException("Mã phòng chiếu '" + form.getRoomCode().trim() + "' đã tồn tại trong hệ thống. Vui lòng nhập mã phòng khác.");
        }
        if (roomDao.existsByRoomName(form.getRoomName(), form.getRoomId())) {
            throw new IllegalArgumentException("Tên phòng chiếu '" + form.getRoomName().trim() + "' đã tồn tại trong hệ thống. Vui lòng đặt tên phòng khác.");
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
     * Room status must be Ready/Screening AND have at least 1 active usable seat (P1-01).
     */
    public boolean isRoomReadyForShowtime(int roomId) {
        Room room = getById(roomId);
        return room.isActiveForShowtimes() && room.getActiveSeatCount() > 0;
    }

    @Transactional
    public void delete(int roomId) {
        Room room = getById(roomId);
        if (roomDao.hasUpcomingShowtimes(roomId)) {
            throw new RoomHasActiveShowtimesException(roomId);
        }
        if (room.getShowtimeCount() > 0) {
            throw new IllegalStateException("Phòng chiếu '" + room.getRoomName() + "' đã có lịch sử " + room.getShowtimeCount() + " suất chiếu trong hệ thống nên không thể xóa để bảo toàn dữ liệu vé và doanh thu. Bạn vui lòng chuyển trạng thái phòng sang 'Bảo trì' nếu không muốn sử dụng phòng này nữa.");
        }
        seatDao.deleteByRoomId(roomId);
        roomDao.delete(roomId);
    }
}
