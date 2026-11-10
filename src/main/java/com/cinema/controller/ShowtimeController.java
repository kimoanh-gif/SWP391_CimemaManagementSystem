package com.cinema.controller;

import com.cinema.exception.InvalidShowtimeException;
import com.cinema.exception.ShowtimeConflictException;
import com.cinema.exception.ShowtimeNotFoundException;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Showtime;
import com.cinema.dto.ShowtimeForm;
import com.cinema.service.MovieService;
import com.cinema.service.RoomService;
import com.cinema.service.ShowtimeService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Screen 59: Showtime Management (Iteration 2 / UC-30).
 * Renders the Thymeleaf management UI with schedule timeline and filters.
 */
@Controller
@RequestMapping("/manager/showtimes")
public class ShowtimeController {

    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;

    public ShowtimeController(ShowtimeService showtimeService, MovieService movieService, RoomService roomService) {
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.roomService = roomService;
    }

    public String viewShowtimes(
            LocalDate date,
            Integer roomId,
            Integer movieId,
            String status,
            Model model,
            HttpServletRequest request
    ) {
        return viewShowtimes(date, null, null, roomId, movieId, status, 1, 10, model, request);
    }

    public String viewShowtimes(
            LocalDate date,
            LocalDate startDate,
            LocalDate endDate,
            Integer roomId,
            Integer movieId,
            String status,
            Model model,
            HttpServletRequest request
    ) {
        return viewShowtimes(date, startDate, endDate, roomId, movieId, status, 1, 10, model, request);
    }

    @GetMapping
    public String viewShowtimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer movieId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        LocalDate filterStartDate = startDate != null ? startDate : date;
        LocalDate filterEndDate = endDate != null ? endDate : date;

        if (filterStartDate != null && filterEndDate != null && filterStartDate.isAfter(filterEndDate)) {
            LocalDate temp = filterStartDate;
            filterStartDate = filterEndDate;
            filterEndDate = temp;
        }

        // If no filter at all, default to today
        if (filterStartDate == null && filterEndDate == null && (roomId == null || roomId <= 0) && (movieId == null || movieId <= 0)) {
            filterStartDate = LocalDate.now();
            filterEndDate = LocalDate.now();
        }

        List<Showtime> showtimes;
        if (startDate != null || endDate != null) {
            showtimes = showtimeService.getAllShowtimes(filterStartDate, filterEndDate, roomId, movieId, status);
        } else {
            showtimes = showtimeService.getAllShowtimes(filterStartDate, roomId, movieId, status);
        }

        // Fetch eligible movies for creating showtimes (BR-07: Upcoming or Now Showing)
        List<Movie> allMovies = movieService.getAllMovies(null, null, null, null);
        List<Movie> eligibleMovies = allMovies.stream()
                .filter(Movie::isActiveForShowtimes)
                .collect(Collectors.toList());

        // Fetch eligible rooms for showtimes (BR-08: Ready or Screening)
        List<Room> allRooms = roomService.getAll(null, null);
        List<Room> eligibleRooms = allRooms.stream()
                .filter(Room::isActiveForShowtimes)
                .collect(Collectors.toList());

        long scheduledCount = showtimes.stream().filter(s -> "Scheduled".equalsIgnoreCase(s.getStatus())).count();
        long showingCount = showtimes.stream().filter(s -> "Showing".equalsIgnoreCase(s.getStatus())).count();
        long stoppedCount = showtimes.stream().filter(s -> "Stopped".equalsIgnoreCase(s.getStatus()) || "Cancelled".equalsIgnoreCase(s.getStatus())).count();

        if (size <= 0) size = 10;
        int totalFiltered = showtimes.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalFiltered / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalFiltered);
        List<Showtime> pagedShowtimes = totalFiltered > 0 ? showtimes.subList(start, end) : java.util.Collections.emptyList();

        model.addAttribute("showtimes", pagedShowtimes);
        model.addAttribute("allMovies", allMovies);
        model.addAttribute("eligibleMovies", eligibleMovies);
        model.addAttribute("allRooms", allRooms);
        model.addAttribute("eligibleRooms", eligibleRooms);

        model.addAttribute("selectedDate", filterStartDate != null ? filterStartDate : LocalDate.now());
        model.addAttribute("startDate", filterStartDate);
        model.addAttribute("endDate", filterEndDate);

        String dateRangeLabel = "Showtimes on selected date";
        String dateRangeSub;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filterStartDate != null && filterEndDate != null) {
            if (filterStartDate.equals(filterEndDate)) {
                dateRangeLabel = "Showtimes on " + filterStartDate.format(dtf);
                dateRangeSub = "Date: " + filterStartDate.format(dtf);
            } else {
                dateRangeLabel = "Showtimes in date range";
                dateRangeSub = "From " + filterStartDate.format(dtf) + " to " + filterEndDate.format(dtf);
            }
        } else if (filterStartDate != null) {
            dateRangeLabel = "Showtimes from date";
            dateRangeSub = "From " + filterStartDate.format(dtf) + " onwards";
        } else if (filterEndDate != null) {
            dateRangeLabel = "Showtimes until date";
            dateRangeSub = "Up to " + filterEndDate.format(dtf);
        } else {
            dateRangeLabel = "All showtimes";
            dateRangeSub = "All scheduled dates";
        }
        model.addAttribute("dateRangeLabel", dateRangeLabel);
        model.addAttribute("dateRangeSub", dateRangeSub);
        model.addAttribute("selectedRoomId", roomId != null ? roomId : 0);
        model.addAttribute("selectedMovieId", movieId != null ? movieId : 0);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("totalShowtimes", totalFiltered);
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("showingCount", showingCount);
        model.addAttribute("stoppedCount", stoppedCount);
        model.addAttribute("cleaningBuffer", ShowtimeService.CLEANING_BUFFER_MINUTES);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalFilteredCount", totalFiltered);
        model.addAttribute("startItem", totalFiltered == 0 ? 0 : start + 1);
        model.addAttribute("endItem", end);

        if (!model.containsAttribute("showtimeForm")) {
            ShowtimeForm defaultForm = new ShowtimeForm();
            if (movieId != null && movieId > 0) {
                defaultForm.setMovieId(movieId);
            }
            if (roomId != null && roomId > 0) {
                defaultForm.setRoomId(roomId);
            }
            model.addAttribute("showtimeForm", defaultForm);
        }

        return "manager/showtimes";
    }

    @GetMapping("/scheduler")
    public String viewRoomScheduler(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        LocalDate minDate = LocalDate.now().plusDays(1);
        LocalDate selectedDate = (date != null && !date.isBefore(minDate)) ? date : minDate;

        List<Room> allRooms = roomService.getAll(null, null);
        List<Room> eligibleRooms = allRooms.stream()
                .filter(Room::isActiveForShowtimes)
                .collect(Collectors.toList());

        Integer activeRoomId = roomId;
        if ((activeRoomId == null || activeRoomId <= 0) && !eligibleRooms.isEmpty()) {
            activeRoomId = eligibleRooms.get(0).getRoomId();
        }

        Room selectedRoom = null;
        List<Showtime> existingShowtimes = new java.util.ArrayList<>();
        if (activeRoomId != null && activeRoomId > 0) {
            final int targetId = activeRoomId;
            selectedRoom = eligibleRooms.stream()
                    .filter(r -> r.getRoomId() == targetId)
                    .findFirst()
                    .orElse(null);
            if (selectedRoom != null) {
                existingShowtimes = showtimeService.getRoomScheduleForDate(activeRoomId, selectedDate);
            }
        }

        List<Movie> allMovies = movieService.getAllMovies(null, null, null, null);
        List<Movie> eligibleMovies = allMovies.stream()
                .filter(Movie::isActiveForShowtimes)
                .collect(Collectors.toList());

        model.addAttribute("eligibleRooms", eligibleRooms);
        model.addAttribute("selectedRoomId", activeRoomId != null ? activeRoomId : 0);
        model.addAttribute("selectedRoom", selectedRoom);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("minDate", minDate);
        model.addAttribute("eligibleMovies", eligibleMovies);
        model.addAttribute("existingShowtimes", existingShowtimes);
        model.addAttribute("cleaningBuffer", ShowtimeService.CLEANING_BUFFER_MINUTES);
        model.addAttribute("trailerDuration", 0);

        return "manager/showtime-scheduler";
    }

    @PostMapping("/save")
    public String saveShowtime(
            @Valid @ModelAttribute("showtimeForm") ShowtimeForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        String targetDate = (form.getStartTime() != null) ? form.getStartTime().toLocalDate().toString() : "";
        String redirectUrl = "redirect:/manager/showtimes" + (targetDate.isEmpty() ? "" : "?date=" + targetDate);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.showtimeForm", bindingResult);
            redirectAttributes.addFlashAttribute("showtimeForm", form);
            String firstErr = bindingResult.getFieldErrors().stream().map(org.springframework.validation.FieldError::getDefaultMessage).findFirst().orElse("Invalid showtime information. Please check again.");
            redirectAttributes.addFlashAttribute("errorMessage", firstErr);
            return redirectUrl;
        }

        try {
            if (form.getShowtimeId() == null || form.getShowtimeId() <= 0) {
                showtimeService.createShowtime(form);
                redirectAttributes.addFlashAttribute("successMessage", "Showtime created successfully!");
            } else {
                showtimeService.updateShowtime(form.getShowtimeId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Showtime updated successfully!");
            }
        } catch (ShowtimeConflictException | InvalidShowtimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("showtimeForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveUserFriendlyErrorMessage(ex));
            redirectAttributes.addFlashAttribute("showtimeForm", form);
        }

        return redirectUrl;
    }

    @PostMapping("/{id}/stop")
    public String stopShowtime(
            @PathVariable int id,
            @RequestParam(required = false) String date,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            showtimeService.stopShowtime(id);
            redirectAttributes.addFlashAttribute("successMessage", "Showtime stopped successfully (BR-54).");
        } catch (InvalidShowtimeException | ShowtimeNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveUserFriendlyErrorMessage(ex));
        }

        return "redirect:/manager/showtimes" + (date != null && !date.isEmpty() ? "?date=" + date : "");
    }

    @PostMapping("/{id}/delete")
    public String deleteShowtime(
            @PathVariable int id,
            @RequestParam(required = false) String date,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            showtimeService.deleteShowtime(id);
            redirectAttributes.addFlashAttribute("successMessage", "Showtime deleted successfully!");
        } catch (InvalidShowtimeException | ShowtimeNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", resolveUserFriendlyErrorMessage(ex));
        }

        return "redirect:/manager/showtimes" + (date != null && !date.isEmpty() ? "?date=" + date : "");
    }

    private String resolveUserFriendlyErrorMessage(Throwable ex) {
        if (ex == null) return "An unexpected error occurred. Please try again.";
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        if (msg == null) return "An unexpected error occurred. Please try again.";

        // Business rule / domain exceptions
        if (ex instanceof ShowtimeConflictException || ex instanceof InvalidShowtimeException || ex instanceof ShowtimeNotFoundException) {
            return ex.getMessage();
        }

        // Database specific friendly translation
        if (msg.contains("The column 'BasePrice' cannot be modified") || msg.contains("computed column") || msg.contains("271")) {
            return "Lỗi cấu hình cột giá vé (BasePrice) trong cơ sở dữ liệu. Hệ thống đã tự động chuyển đổi, vui lòng thử lưu lại.";
        }
        if (msg.contains("FK_Showtime_Movie") || (msg.contains("FOREIGN KEY") && msg.contains("Movie"))) {
            return "Phim đã chọn không tồn tại hoặc không hợp lệ.";
        }
        if (msg.contains("FK_Showtime_Room") || (msg.contains("FOREIGN KEY") && msg.contains("Room"))) {
            return "Phòng chiếu đã chọn không tồn tại hoặc không hợp lệ.";
        }
        if (msg.contains("CK_Showtime_TimeRange")) {
            return "Thời gian kết thúc suất chiếu phải sau thời gian bắt đầu.";
        }
        if (msg.contains("CK_Showtime_BasePrice")) {
            return "Giá vé cơ bản không được nhỏ hơn 0.";
        }
        if (msg.contains("timed out") || msg.contains("Connection refused") || msg.contains("Communications link failure")) {
            return "Không thể kết nối đến máy chủ cơ sở dữ liệu. Vui lòng kiểm tra dịch vụ SQL Server.";
        }

        // Clean raw technical SQL statement dump
        if (msg.contains("PreparedStatementCallback") || msg.contains("SQLException") || msg.contains("SQL state")) {
            int errIdx = msg.indexOf("error code");
            if (errIdx >= 0) {
                int colon = msg.indexOf(":", errIdx);
                if (colon >= 0 && colon + 1 < msg.length()) {
                    return "Lỗi cơ sở dữ liệu: " + msg.substring(colon + 1).trim();
                }
            }
            return "Đã xảy ra lỗi khi lưu vào cơ sở dữ liệu. Vui lòng kiểm tra lại thông tin suất chiếu.";
        }

        return msg;
    }
}
