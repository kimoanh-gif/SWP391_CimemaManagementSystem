package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Action: Submit Movie Review ( POST /movies/{id}/reviews ) — from the Movie Details screen.
 * Use case: UC16 — Submit Movie Review
 *
 * Kept in its own controller so the Movie Details screen controller stays read-only.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class MovieReviewController {

    private final MovieCatalogService movieService;
    private final SessionService sessionService;

    public MovieReviewController(MovieCatalogService movieService, SessionService sessionService) {
        this.movieService = movieService;
        this.sessionService = sessionService;
    }

    @PostMapping("/movies/{id}/reviews")
    public String submitReview(
            @PathVariable("id") int movieId,
            @RequestParam(name = "rating", defaultValue = "5") int rating,
            @RequestParam(name = "content", required = false) String content,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        if (user == null || !"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in with a customer account to review this movie.");
            return "redirect:/login?redirect=/movies/" + movieId;
        }

        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please choose a rating from 1 to 5 stars.");
            return "redirect:/movies/" + movieId + "#reviews";
        }
        if (content == null || content.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please write a few words about the movie.");
            return "redirect:/movies/" + movieId + "#reviews";
        }

        boolean success;
        try {
            success = movieService.addReview(user.getAccountId(), movieId, rating, content.trim());
        } catch (DataAccessException ex) {
            success = false;
        }

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for submitting your review.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Your review could not be saved. Please try again.");
        }
        return "redirect:/movies/" + movieId + "#reviews";
    }
}
