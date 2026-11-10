package com.cinema.controller;

import com.cinema.exception.DuplicateGenreException;
import com.cinema.exception.GenreAssociationException;
import com.cinema.exception.GenreNotFoundException;
import com.cinema.model.Genre;
import com.cinema.dto.GenreForm;
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
     * Backward-compatible overload for direct invocations in tests.
     */
    public String viewGenreList(
            String keyword,
            String status,
            Model model,
            HttpServletRequest request
    ) {
        return viewGenreList(keyword, status, 1, 10, model, request);
    }

    /**
     * Displays the Genre Management screen with search, filter, metrics, and pagination.
     */
    @GetMapping
    public String viewGenreList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<Genre> genres = genreService.getAllGenres(keyword, status);

        // Compute summary metrics across all matching genres before pagination
        long totalCount = genres.size();
        long activeCount = genres.stream().filter(Genre::isActive).count();
        long inactiveCount = totalCount - activeCount;
        long genresWithMovies = genres.stream().filter(g -> g.getMovieCount() > 0).count();

        if (size <= 0) size = 10;
        int totalFiltered = genres.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalFiltered / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalFiltered);
        List<Genre> pagedGenres = totalFiltered > 0 ? genres.subList(start, end) : java.util.Collections.emptyList();

        model.addAttribute("genres", pagedGenres);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("genresWithMovies", genresWithMovies);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalFilteredCount", totalFiltered);
        model.addAttribute("startItem", totalFiltered == 0 ? 0 : start + 1);
        model.addAttribute("endItem", end);

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
            String firstErr = bindingResult.getFieldErrors().stream().map(org.springframework.validation.FieldError::getDefaultMessage).findFirst().orElse("Invalid input data. Please check your information.");
            redirectAttributes.addFlashAttribute("errorMessage", firstErr);
            return "redirect:/manager/genres";
        }

        try {
            if (form.getId() == null || form.getId() <= 0) {
                genreService.createGenre(form);
                redirectAttributes.addFlashAttribute("successMessage", "Genre '" + form.getName() + "' created successfully!");
            } else {
                genreService.updateGenre(form.getId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Genre '" + form.getName() + "' updated successfully!");
            }
        } catch (DuplicateGenreException | GenreAssociationException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("genreForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + ex.getMessage());
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
            String statusText = genre.isActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Genre '" + genre.getName() + "' has been successfully " + statusText + "."
            );
        } catch (GenreAssociationException | GenreNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot update status: " + ex.getMessage());
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
            redirectAttributes.addFlashAttribute("successMessage", "Genre deleted successfully.");
        } catch (GenreAssociationException | GenreNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete genre: " + ex.getMessage());
        }

        return "redirect:/manager/genres";
    }
}
