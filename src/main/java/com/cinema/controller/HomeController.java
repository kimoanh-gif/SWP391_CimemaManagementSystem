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
import java.util.Map;
import java.util.Optional;

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
        List<Movie> nowShowing = movieService.getNowShowingMovies(8);
        model.addAttribute("nowShowingMovies", nowShowing);

        Map<String, String> heroBackdrops = Map.ofEntries(
                Map.entry("Trái Tim Quái Thú", "https://cdn.moveek.com/storage/media/cache/large/6a538be95e054699735537.webp"),
                Map.entry("Án Mạng Xém Hoàn Hảo", "https://cdn.moveek.com/storage/media/cache/large/6a8d5b4137e8c939819537.webp"),
                Map.entry("Street Fighter", "https://cdn.moveek.com/storage/media/cache/large/6a538ba81c7f6196807171.webp"),
                Map.entry("Quỷ Ăn Tạng 4: Hổ Tinh", "https://cdn.moveek.com/storage/media/cache/large/6aab6126c0a45774834154.webp"),
                Map.entry("Bambi: Lãnh Địa Săn Mồi", "https://cdn.moveek.com/storage/media/cache/large/6accc26017036308778097.webp"),
                Map.entry("Cõi Nguyền", "https://cdn.moveek.com/storage/media/cache/large/6ac85c33c3ca7545011688.webp")
        );
        nowShowing.forEach(movie -> movie.setHeroImageUrl(
                heroBackdrops.getOrDefault(movie.getTitle(), movie.getPosterUrl())));
        model.addAttribute("featuredMovies", nowShowing);

        List<Movie> upcomingMovies = movieService.getUpcomingMovies(15);
        model.addAttribute("upcomingMovies", upcomingMovies);

        List<PublicShowtime> todayShowtimes = showtimeService.getTodayShowtimes(8);
        model.addAttribute("todayShowtimes", todayShowtimes);

        List<Promotion> promotions = promotionNewsService.getLatestPromotions(4);
        model.addAttribute("promotions", promotions);

        List<NewsArticle> newsList = promotionNewsService.getLatestNews(3);
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
