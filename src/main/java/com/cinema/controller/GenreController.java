package com.cinema.controller;

import com.cinema.exception.DuplicateGenreException;
import com.cinema.exception.GenreAssociationException;
import com.cinema.exception.GenreNotFoundException;
import com.cinema.model.Genre;
import com.cinema.model.GenreForm;
import com.cinema.service.GenreService;
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
 * Controller for Screen 55: Genre Management (Iteration 2).
 * Serves the manager Thymeleaf template and processes form submissions.
 */
@Controller
@RequestMapping("/manager/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    /**
     * Displays the Genre Management screen with search, filter, and metrics.
     */
    @GetMapping
    public String viewGenreList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<Genre> genres = genreService.getAllGenres(keyword, status);

        // Compute summary metrics
        long totalCount = genres.size();
        long activeCount = genres.stream().filter(Genre::isActive).count();
        long inactiveCount = totalCount - activeCount;
        long genresWithMovies = genres.stream().filter(g -> g.getMovieCount() > 0).count();

        model.addAttribute("genres", genres);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("genresWithMovies", genresWithMovies);

        if (!model.containsAttribute("genreForm")) {
            model.addAttribute("genreForm", new GenreForm());
        }

        return "manager/genres";
    }

    /**
     * Handles creation and update of a genre.
     */
    @PostMapping("/save")
    public String saveGenre(
            @Valid @ModelAttribute("genreForm") GenreForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.genreForm",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("genreForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu nhập không hợp lệ. Vui lòng kiểm tra lại thông tin.");
            return "redirect:/manager/genres";
        }

        try {
            if (form.getId() == null || form.getId() <= 0) {
                genreService.createGenre(form);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm thể loại mới '" + form.getName() + "' thành công!");
            } else {
                genreService.updateGenre(form.getId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thể loại '" + form.getName() + "' thành công!");
            }
        } catch (DuplicateGenreException | GenreAssociationException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("genreForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("genreForm", form);
        }

        return "redirect:/manager/genres";
    }

    /**
     * Toggles genre status between ACTIVE and INACTIVE (Deactivation management).
     */
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            Genre genre = genreService.toggleStatus(id);
            String statusText = genre.isActive() ? "kích hoạt" : "hủy kích hoạt";
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã " + statusText + " thành công thể loại '" + genre.getName() + "'."
            );
        } catch (GenreAssociationException | GenreNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + ex.getMessage());
        }

        return "redirect:/manager/genres";
    }

    /**
     * Deletes a genre if no movie associations exist.
     */
    @PostMapping("/{id}/delete")
    public String deleteGenre(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            genreService.deleteGenre(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thể loại thành công.");
        } catch (GenreAssociationException | GenreNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thể loại: " + ex.getMessage());
        }

        return "redirect:/manager/genres";
    }
}
