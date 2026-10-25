package com.cinema.controller;

import com.cinema.exception.InvalidMovieStatusException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.dto.MovieForm;
import com.cinema.service.GenreService;
import com.cinema.service.MovieService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * Controller for Screen 54: Movie Management (UC-26 / SWP391 Iteration 2).
 * Serves the manager Thymeleaf template and processes form actions.
 */
@Controller
@RequestMapping("/manager/movies")
public class MovieController {

    private final MovieService movieService;
    private final GenreService genreService;

    public MovieController(MovieService movieService, GenreService genreService) {
        this.movieService = movieService;
        this.genreService = genreService;
    }

    /**
     * Displays the Movie Management screen with search, filters, statistics, and movie list.
     */
    @GetMapping
    public String viewMovieList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false, defaultValue = "ALL") String ageRating,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<Movie> movies = movieService.getAllMovies(keyword, genreId, ageRating, status);
        List<Genre> allGenres = genreService.getAllGenres(null, "ACTIVE");

        // Compute system-wide metrics so top KPI cards remain true system counts regardless of filters
        List<Movie> allSystemMovies = movieService.getAllMovies(null, null, "ALL", "ALL");
        long totalCount = allSystemMovies.size();
        long nowShowingCount = allSystemMovies.stream().filter(m -> "Now Showing".equalsIgnoreCase(m.getStatus())).count();
        long upcomingCount = allSystemMovies.stream().filter(m -> "Upcoming".equalsIgnoreCase(m.getStatus())).count();
        long archivedCount = allSystemMovies.stream().filter(m -> "Archived".equalsIgnoreCase(m.getStatus()) || "Ended".equalsIgnoreCase(m.getStatus())).count();

        model.addAttribute("movies", movies);
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedGenreId", genreId != null ? genreId : 0);
        model.addAttribute("selectedAgeRating", ageRating);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("filteredCount", movies.size());
        model.addAttribute("nowShowingCount", nowShowingCount);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("archivedCount", archivedCount);

        if (!model.containsAttribute("movieForm")) {
            model.addAttribute("movieForm", new MovieForm());
        }

        return "manager/movies";
    }

    /**
     * Handles adding and updating movie information.
     */
    @PostMapping("/save")
    public String saveMovie(
            @Valid @ModelAttribute("movieForm") MovieForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.movieForm",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("movieForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu nhập không hợp lệ. Vui lòng kiểm tra lại thông tin.");
            return "redirect:/manager/movies";
        }

        try {
            if (form.getId() == null || form.getId() <= 0) {
                movieService.createMovie(form);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm phim mới '" + form.getTitle() + "' thành công!");
            } else {
                movieService.updateMovie(form.getId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin phim '" + form.getTitle() + "' thành công!");
            }
        } catch (MovieHasActiveShowtimesException | InvalidMovieStatusException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("movieForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("movieForm", form);
        }

        return "redirect:/manager/movies";
    }

    /**
     * Deactivates a movie without physically deleting it (BR-37).
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateMovie(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            Movie movie = movieService.deactivateMovie(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã hủy kích hoạt và lưu trữ phim '" + movie.getTitle() + "' thành công."
            );
        } catch (MovieHasActiveShowtimesException | MovieNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy kích hoạt phim: " + ex.getMessage());
        }

        return "redirect:/manager/movies";
    }

    /**
     * Updates movie status quickly from the list.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable int id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            Movie movie = movieService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã chuyển trạng thái phim '" + movie.getTitle() + "' sang " + movie.getStatus() + "."
            );
        } catch (MovieHasActiveShowtimesException | InvalidMovieStatusException | MovieNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + ex.getMessage());
        }

        return "redirect:/manager/movies";
    }

    /**
     * Reactivates an archived movie back to active status.
     */
    @PostMapping("/{id}/reactivate")
    public String reactivateMovie(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            Movie movie = movieService.reactivateMovie(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã mở lại và chuyển phim '" + movie.getTitle() + "' sang trạng thái " + movie.getStatus() + "."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể kích hoạt lại phim: " + ex.getMessage());
        }

        return "redirect:/manager/movies";
    }

    /**
     * Deletes a movie or deactivates it if historical showtimes exist.
     */
    @PostMapping("/{id}/delete")
    public String deleteMovie(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            movieService.deleteMovie(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã xóa phim thành công."
            );
        } catch (MovieHasActiveShowtimesException | MovieNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa phim: " + ex.getMessage());
        }

        return "redirect:/manager/movies";
    }
}
