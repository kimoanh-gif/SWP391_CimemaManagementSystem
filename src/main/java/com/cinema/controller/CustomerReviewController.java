package com.cinema.controller;

import com.cinema.service.MovieCatalogService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerReviewController {
    private final MovieCatalogService movieCatalogService;
    private final SessionService sessionService;

    public CustomerReviewController(MovieCatalogService movieCatalogService, SessionService sessionService) {
        this.movieCatalogService = movieCatalogService;
        this.sessionService = sessionService;
    }

    @GetMapping("/customer/reviews")
    public String reviews(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "CUSTOMER")) return "redirect:/login";
        int accountId = sessionService.currentAccountId(session);
        model.addAttribute("currentUser", sessionService.getCurrentUser(session));
        model.addAttribute("movies", movieCatalogService.getReviewableMovies(accountId));
        return "customer/reviews";
    }

    @PostMapping("/customer/reviews/{movieId}")
    public String saveReview(@PathVariable int movieId,
                             @RequestParam int bookingId,
                             @RequestParam(defaultValue = "5") int rating,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!sessionService.hasRole(session, "CUSTOMER")) return "redirect:/login";
        String cleanContent = content == null ? "" : content.trim();
        if (cleanContent.isBlank() || cleanContent.length() > 1000) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review content must contain between 1 and 1000 characters.");
            return "redirect:/customer/reviews";
        }
        boolean saved = movieCatalogService.addReview(
                sessionService.currentAccountId(session), movieId, bookingId, rating, cleanContent);
        redirectAttributes.addFlashAttribute(saved ? "successMessage" : "errorMessage",
                saved ? "Your review has been saved. You can edit this same review at any time."
                        : "This booking is not eligible for review.");
        return "redirect:/customer/reviews";
    }
}
