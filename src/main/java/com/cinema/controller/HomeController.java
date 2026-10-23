package com.cinema.controller;

import com.cinema.model.Movie;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PromotionNewsService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Screen: Home Page ( / , /home )
 * Functional requirements:
 * - View Movie List: featured hero slider, Now Showing, Coming Soon
 * - View Showtimes: today's showtimes preview and quick booking bar
 * - View Promotions &amp; News: latest promotions and cinema news
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class HomeController {

    static final int FEATURED_LIMIT = 5;
    static final int NOW_SHOWING_LIMIT = 8;
    static final int COMING_SOON_LIMIT = 6;
    static final int TODAY_SHOWTIME_LIMIT = 8;
    static final int PROMOTION_LIMIT = 4;
    static final int NEWS_LIMIT = 3;
    static final int QUICK_BOOKING_DAYS = 7;

    private final MovieCatalogService movieService;
    private final PublicShowtimeService showtimeService;
    private final PromotionNewsService promotionNewsService;
    private final PublicPageModelHelper pageHelper;

    public HomeController(MovieCatalogService movieService,
                          PublicShowtimeService showtimeService,
                          PromotionNewsService promotionNewsService,
                          PublicPageModelHelper pageHelper) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.promotionNewsService = promotionNewsService;
        this.pageHelper = pageHelper;
    }

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        pageHelper.populateHeader(session, model, "home");

        // 1. Movies (BR-ML-02: Now Showing and Coming Soon are separate categories)
        List<Movie> nowShowing = pageHelper.loadSafely(model,
                () -> movieService.getNowShowingMovies(NOW_SHOWING_LIMIT), List.of());
        List<Movie> comingSoon = pageHelper.loadSafely(model,
                () -> movieService.getComingSoonMovies(COMING_SOON_LIMIT), List.of());
        List<Movie> featured = pageHelper.loadSafely(model,
                () -> movieService.getFeaturedMovies(FEATURED_LIMIT), List.of());
        if (featured.isEmpty()) {
            featured = comingSoon.stream().limit(FEATURED_LIMIT).toList();
        }
        model.addAttribute("featuredMovies", featured);
        model.addAttribute("nowShowingMovies", nowShowing);
        model.addAttribute("comingSoonMovies", comingSoon);

        // 2. Showtimes
        model.addAttribute("todayShowtimes", pageHelper.loadSafely(model,
                () -> showtimeService.getTodayShowtimes(TODAY_SHOWTIME_LIMIT), List.of()));

        // 3. Promotions & News (BR-PN-06: one empty type never hides the other)
        model.addAttribute("promotions", pageHelper.loadSafely(model,
                () -> promotionNewsService.getLatestPromotions(PROMOTION_LIMIT), List.of()));
        model.addAttribute("newsList", pageHelper.loadSafely(model,
                () -> promotionNewsService.getLatestNews(NEWS_LIMIT), List.of()));

        // 4. Quick booking bar: every public movie (pre-sale showtimes may exist for Coming Soon)
        model.addAttribute("allMovies", pageHelper.loadSafely(model,
                movieService::getAllPublicMovies, List.of()));
        model.addAttribute("upcomingDates", showtimeService.getUpcomingDates(QUICK_BOOKING_DAYS));

        return "index";
    }
}
