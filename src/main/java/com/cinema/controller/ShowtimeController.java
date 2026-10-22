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

    @GetMapping
    public String viewShowtimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer movieId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        LocalDate filterDate = (date != null) ? date : LocalDate.now();

        List<Showtime> showtimes = showtimeService.getAllShowtimes(filterDate, roomId, movieId, status);

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

        model.addAttribute("showtimes", showtimes);
        model.addAttribute("allMovies", allMovies);
        model.addAttribute("eligibleMovies", eligibleMovies);
        model.addAttribute("allRooms", allRooms);
        model.addAttribute("eligibleRooms", eligibleRooms);

        model.addAttribute("selectedDate", filterDate);
        model.addAttribute("selectedRoomId", roomId != null ? roomId : 0);
        model.addAttribute("selectedMovieId", movieId != null ? movieId : 0);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("totalShowtimes", showtimes.size());
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("showingCount", showingCount);
        model.addAttribute("stoppedCount", stoppedCount);
        model.addAttribute("cleaningBuffer", ShowtimeService.CLEANING_BUFFER_MINUTES);

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

    @PostMapping("/save")
    public String saveShowtime(
            @Valid @ModelAttribute("showtimeForm") ShowtimeForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.showtimeForm", bindingResult);
            redirectAttributes.addFlashAttribute("showtimeForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin suất chiếu không hợp lệ. Vui lòng kiểm tra lại.");
            return "redirect:/manager/showtimes";
        }

        try {
            if (form.getShowtimeId() == null || form.getShowtimeId() <= 0) {
                showtimeService.createShowtime(form);
                redirectAttributes.addFlashAttribute("successMessage", "Tạo suất chiếu mới thành công!");
            } else {
                showtimeService.updateShowtime(form.getShowtimeId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật suất chiếu thành công!");
            }
        } catch (ShowtimeConflictException | InvalidShowtimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("showtimeForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("showtimeForm", form);
        }

        return "redirect:/manager/showtimes";
    }

    @PostMapping("/{id}/stop")
    public String stopShowtime(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            showtimeService.stopShowtime(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã dừng suất chiếu thành công (BR-54).");
        } catch (InvalidShowtimeException | ShowtimeNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/manager/showtimes";
    }

    @PostMapping("/{id}/delete")
    public String deleteShowtime(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            showtimeService.deleteShowtime(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa suất chiếu thành công!");
        } catch (InvalidShowtimeException | ShowtimeNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/manager/showtimes";
    }
}
