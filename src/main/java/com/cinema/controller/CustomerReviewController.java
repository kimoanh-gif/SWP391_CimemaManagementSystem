package com.cinema.controller;

import com.cinema.service.MovieCatalogService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}
