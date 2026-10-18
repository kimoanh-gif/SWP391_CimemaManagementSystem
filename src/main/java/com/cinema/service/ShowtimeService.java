package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.exception.InvalidShowtimeException;
import com.cinema.exception.ShowtimeConflictException;
import com.cinema.exception.ShowtimeNotFoundException;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Showtime;
import com.cinema.model.ShowtimeForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for Showtime Management (Screen 59 / UC-30).
 * Enforces:
 * - BR-07: Only Upcoming and Now Showing movies may be scheduled.
 * - BR-08: Only active cinema rooms (Ready/Screening) may receive showtimes.
 * - BR-09: Overlap checking (newStart < existingEnd AND newEnd > existingStart) + cleaning buffer.
 * - BR-54: Showtime with Held/Paid bookings cannot change movie, room, start time or be stopped.
 * - BR-55: End time = Start time + Movie duration. New showtime must start in the future.
 * - P1-01: Room must have at least 1 active usable seat to be scheduled.
 */
@Service
public class ShowtimeService {

    public static final int CLEANING_BUFFER_MINUTES = 15; // 15-minute room cleaning buffer per SRS

    private final ShowtimeDao showtimeDao;
    private final MovieDao movieDao;
    private final RoomDao roomDao;
    private final SeatDao seatDao;

    public ShowtimeService(ShowtimeDao showtimeDao, MovieDao movieDao, RoomDao roomDao) {
        this(showtimeDao, movieDao, roomDao, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ShowtimeService(ShowtimeDao showtimeDao, MovieDao movieDao, RoomDao roomDao, SeatDao seatDao) {
        this.showtimeDao = showtimeDao;
        this.movieDao = movieDao;
        this.roomDao = roomDao;
        this.seatDao = seatDao;
    }

    public List<Showtime> getAllShowtimes(LocalDate date, Integer roomId, Integer movieId, String status) {
        return showtimeDao.findAll(date, roomId, movieId, status);
    }

    public Showtime getShowtimeById(int id) {
        return showtimeDao.findById(id)
                .orElseThrow(() -> new ShowtimeNotFoundException("Không tìm thấy suất chiếu với ID: " + id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Showtime createShowtime(ShowtimeForm form) {
        if (form.getRoomId() != null && form.getRoomId() > 0) {
            roomDao.lockRoomForUpdate(form.getRoomId());
        }
        validateShowtime(form, null);

        showtimeDao.insert(form);

        // Fetch newly created showtime by matching criteria
        List<Showtime> createdList = showtimeDao.findOverlappingShowtimes(
                form.getRoomId(),
                form.getStartTime(),
                form.getEndTime(),
                null
        );
        if (!createdList.isEmpty()) {
            return createdList.get(0);
        }
        return showtimeDao.findAll(form.getStartTime().toLocalDate(), form.getRoomId(), form.getMovieId(), null).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Showtime updateShowtime(int id, ShowtimeForm form) {
        Showtime existing = getShowtimeById(id);

        if (form.getRoomId() != null && form.getRoomId() > 0) {
            roomDao.lockRoomForUpdate(form.getRoomId());
            if (existing.getRoomId() != null && !existing.getRoomId().equals(form.getRoomId())) {
                roomDao.lockRoomForUpdate(existing.getRoomId());
            }
        }

        // BR-54: Showtime with bookings cannot change movie, room, start time, or be stopped
        if (showtimeDao.hasActiveBookings(id)) {
            if (!existing.getMovieId().equals(form.getMovieId())
                    || !existing.getRoomId().equals(form.getRoomId())
                    || !existing.getStartTime().equals(form.getStartTime())) {
                throw new InvalidShowtimeException("Suất chiếu đã có vé đặt, không được phép thay đổi Phim, Phòng chiếu hoặc Thời gian bắt đầu (BR-54).");
            }
        }

        validateShowtime(form, id);

        showtimeDao.update(id, form);
        return getShowtimeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Showtime stopShowtime(int id) {
        Showtime existing = getShowtimeById(id);

        // BR-54: cannot stop if there are active bookings
        if (showtimeDao.hasActiveBookings(id)) {
            throw new InvalidShowtimeException("Suất chiếu đã có vé đặt, không thể hủy hoặc dừng chiếu (BR-54).");
        }

        showtimeDao.updateStatus(id, "Stopped");
        return getShowtimeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Showtime cancelShowtime(int id) {
        Showtime existing = getShowtimeById(id);

        if (showtimeDao.hasActiveBookings(id)) {
            throw new InvalidShowtimeException("Suất chiếu đã có vé đặt, không thể hủy lịch chiếu (BR-54).");
        }

        showtimeDao.updateStatus(id, "Cancelled");
        return getShowtimeById(id);
    }

    /**
     * Comprehensive validation per SRS and business rules.
     */
    public void validateShowtime(ShowtimeForm form, Integer currentShowtimeId) {
        if (form.getMovieId() == null || form.getMovieId() <= 0) {
            throw new InvalidShowtimeException("Vui lòng chọn phim hợp lệ.");
        }
        if (form.getRoomId() == null || form.getRoomId() <= 0) {
            throw new InvalidShowtimeException("Vui lòng chọn phòng chiếu hợp lệ.");
        }
        if (form.getStartTime() == null) {
            throw new InvalidShowtimeException("Vui lòng chọn thời gian bắt đầu.");
        }
        if (form.getBaseTicketPrice() == null || form.getBaseTicketPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidShowtimeException("Giá vé cơ sở không được âm.");
        }

        // BR-55: A new showtime must start in the future (for new showtimes)
        if (currentShowtimeId == null && form.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidShowtimeException("Thời gian bắt đầu suất chiếu mới phải ở tương lai (BR-55).");
        }

        // Validate Movie & BR-07
        Movie movie = movieDao.findById(form.getMovieId())
                .orElseThrow(() -> new InvalidShowtimeException("Phim được chọn không tồn tại trong hệ thống."));

        if (!movie.isActiveForShowtimes()) {
            throw new InvalidShowtimeException("Chỉ những phim ở trạng thái 'Đang chiếu' (Now Showing) hoặc 'Sắp chiếu' (Upcoming) mới được xếp lịch chiếu (BR-07). Phim '"
                    + movie.getTitle() + "' đang ở trạng thái: " + movie.getStatus());
        }

        // Validate Room & BR-08
        Room room = roomDao.findById(form.getRoomId())
                .orElseThrow(() -> new InvalidShowtimeException("Phòng chiếu được chọn không tồn tại trong hệ thống."));

        if (!room.isActiveForShowtimes()) {
            throw new InvalidShowtimeException("Phòng chiếu '" + room.getRoomName() + "' không ở trạng thái sẵn sàng đón khách (BR-08). Trạng thái hiện tại: " + room.getStatus());
        }

        // P1-01: Room readiness and seat availability validation (AC01, AC02)
        if (seatDao != null) {
            int usableSeats = seatDao.countActiveSeatsByRoomId(form.getRoomId());
            if (usableSeats <= 0) {
                throw new InvalidShowtimeException("Phòng chiếu '" + room.getRoomName() + "' chưa có sơ đồ ghế khả dụng. Vui lòng thiết lập sơ đồ ghế trước khi tạo suất chiếu.");
            }
        }

        // Calculate and auto-set EndTime = StartTime + Movie Duration (BR-55)
        int duration = (movie.getDuration() != null && movie.getDuration() > 0) ? movie.getDuration() : 120;
        form.setEndTime(form.getStartTime().plusMinutes(duration));

        // BR-09: Overlap checking (newStart < existingEnd + 15m AND existingStart < newEnd + 15m)
        List<Showtime> conflicts = showtimeDao.findOverlappingShowtimes(
                form.getRoomId(),
                form.getStartTime(),
                form.getEndTime(),
                currentShowtimeId
        );

        if (!conflicts.isEmpty()) {
            Showtime conflict = conflicts.get(0);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            throw new ShowtimeConflictException(
                    "Trùng lịch chiếu hoặc thời gian dọn phòng (BR-09)! Phòng '" + room.getRoomName() +
                            "' đã có suất chiếu phim '" + conflict.getMovieTitle() +
                            "' từ " + conflict.getStartTime().format(dtf) +
                            " đến " + conflict.getEndTime().format(dtf) +
                            " (kèm 15 phút dọn dẹp)."
            );
        }
    }

}
