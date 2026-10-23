package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Customer;
import com.cinema.model.Movie;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import com.cinema.model.PublicShowtime;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PromotionNewsService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Screen 21: Home Page
 * Functional Requirements:
 * - View Movie List (Featured carousel, Now Showing, Coming Soon)
 * - View Showtimes (Today's preview, quick booking date/movie selector)
 * - View Promotions & News (Hot discount vouchers, cinema news & events)
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class HomeController {

    private final MovieCatalogService movieService;
    private final PublicShowtimeService showtimeService;
    private final PromotionNewsService promotionNewsService;
    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public HomeController(MovieCatalogService movieService,
                          PublicShowtimeService showtimeService,
                          PromotionNewsService promotionNewsService,
                          SessionService sessionService,
                          CustomerDAO customerDAO) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.promotionNewsService = promotionNewsService;
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        // Homepage catalog sections.
        List<Movie> nowShowing = movieService.getNowShowingMovies(8);
        model.addAttribute("nowShowingMovies", nowShowing);

        // The hero exclusively promotes upcoming releases.
        List<Movie> comingSoon = movieService.getComingSoonMovies(15);
        model.addAttribute("featuredMovies", comingSoon);
        model.addAttribute("comingSoonMovies", comingSoon);

        // 3. Today's Showtimes preview
        List<PublicShowtime> todayShowtimes = showtimeService.getTodayShowtimes(8);
        model.addAttribute("todayShowtimes", todayShowtimes);

        // 4. Featured Promotions
        List<Promotion> promotions = promotionNewsService.getLatestPromotions(4);
        model.addAttribute("promotions", promotions);

        // 5. Latest Cinema News
        List<NewsArticle> newsList = promotionNewsService.getLatestNews(3);
        model.addAttribute("newsList", newsList);

        // 6. Quick Booking Bar options
        model.addAttribute("allMovies", movieService.searchMovies(null, null, "ALL", "newest"));
        model.addAttribute("upcomingDates", showtimeService.getUpcomingDates(7));

        // 7. Session user info
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
