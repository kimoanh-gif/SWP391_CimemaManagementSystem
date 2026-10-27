package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Customer;
import com.cinema.model.Movie;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import com.cinema.model.PublicShowtime;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.NewsPublicService;
import com.cinema.service.PromotionPublicService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final MovieCatalogService movieService;
    private final PublicShowtimeService showtimeService;
    private final PromotionPublicService promotionService;
    private final NewsPublicService newsService;
    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public HomeController(MovieCatalogService movieService,
                          PublicShowtimeService showtimeService,
                          PromotionPublicService promotionService,
                          NewsPublicService newsService,
                          SessionService sessionService,
                          CustomerDAO customerDAO) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.promotionService = promotionService;
        this.newsService = newsService;
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        List<Movie> nowShowing = movieService.getNowShowingMovies(0);
        model.addAttribute("nowShowingMovies", nowShowing);

        List<Movie> topGrossingMovies = movieService.getFeaturedMovies(10);
        model.addAttribute("featuredMovies", topGrossingMovies);
        model.addAttribute("topGrossingMovies", topGrossingMovies);

        List<Movie> upcomingMovies = movieService.getUpcomingMovies(0);
        model.addAttribute("upcomingMovies", upcomingMovies);

        List<PublicShowtime> todayShowtimes = showtimeService.getTodayShowtimes(8);
        model.addAttribute("todayShowtimes", todayShowtimes);

        List<Promotion> promotions = promotionService.getLatestPromotions(4);
        model.addAttribute("promotions", promotions);

        List<NewsArticle> newsList = newsService.getLatestNews(3);
        model.addAttribute("newsList", newsList);

        model.addAttribute("allMovies", movieService.searchMovies(null, null, "ALL", "newest"));
        model.addAttribute("upcomingDates", showtimeService.getUpcomingDates(7));

        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Optional<Customer> custOpt = customerDAO.findByAccountId(user.getAccountId());
            custOpt.ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }

        model.addAttribute("activeNav", "home");
        return "index";
    }
}
