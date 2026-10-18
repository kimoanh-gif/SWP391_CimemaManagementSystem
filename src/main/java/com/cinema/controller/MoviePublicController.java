package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Customer;
import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.model.MovieReview;
import com.cinema.model.PublicShowtime;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Screen 22: Movie List (/movies)
 * Screen 23: Movie Details (/movies/{id})
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
@RequestMapping("/movies")
public class MoviePublicController {

    private final MovieCatalogService movieService;
    private final PublicShowtimeService showtimeService;
    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public MoviePublicController(MovieCatalogService movieService,
                                 PublicShowtimeService showtimeService,
                                 SessionService sessionService,
                                 CustomerDAO customerDAO) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    /**
     * Screen 22: Movie List
     */
    @GetMapping
    public String listMovies(
            @RequestParam(name = "type", required = false, defaultValue = "all") String type,
            @RequestParam(name = "genreId", required = false) Integer genreId,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sortBy,
            HttpSession session,
            Model model
    ) {
        List<Movie> movies = movieService.searchMovies(keyword, genreId, type, sortBy);
        List<Genre> genres = movieService.getAllGenres();

        model.addAttribute("movies", movies);
        model.addAttribute("genres", genres);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedGenreId", genreId);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("sortBy", sortBy);

        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        populateCustomerPoints(session, model);

        model.addAttribute("activeNav", "movies");
        return "customer/movies/list";
    }

    /**
     * Screen 23: Movie Details
     */
    @GetMapping("/{id}")
    public String movieDetails(
            @PathVariable("id") int movieId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Movie> movieOpt = movieService.getMovieById(movieId);
        if (movieOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phim yêu cầu.");
            return "redirect:/movies";
        }
        Movie movie = movieOpt.get();
        model.addAttribute("movie", movie);

        // Date selection for showtimes
        if (showDate == null) {
            showDate = LocalDate.now();
        }
        model.addAttribute("selectedDate", showDate);

        // Upcoming available dates for this movie
        List<LocalDate> availableDates = showtimeService.getAvailableDatesForMovie(movieId);
        model.addAttribute("availableDates", availableDates);

        // Showtimes on selected date
        List<PublicShowtime> showtimes = showtimeService.getShowtimesForMovie(movieId, showDate);
        model.addAttribute("showtimes", showtimes);

        // Movie Reviews
        List<MovieReview> reviews = movieService.getMovieReviews(movieId);
        model.addAttribute("reviews", reviews);

        // Related movies
        List<Movie> relatedMovies = movieService.getRelatedMovies(movieId, 4);
        model.addAttribute("relatedMovies", relatedMovies);

        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        populateCustomerPoints(session, model);

        model.addAttribute("activeNav", "movies");
        return "customer/movies/details";
    }

    /**
     * Screen 23: Customer Review Submission
     */
    @PostMapping("/{id}/reviews")
    public String submitReview(
            @PathVariable("id") int movieId,
            @RequestParam(name = "rating", defaultValue = "5") int rating,
            @RequestParam(name = "content", required = false) String content,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        if (user == null || !"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập bằng tài khoản Khách hàng để đánh giá phim.");
            return "redirect:/login?redirect=/movies/" + movieId;
        }

        boolean success = movieService.addReview(user.getAccountId(), movieId, rating, content != null ? content.trim() : "");
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá cho bộ phim!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể lưu đánh giá. Vui lòng thử lại sau.");
        }
        return "redirect:/movies/" + movieId + "#reviews";
    }

    private void populateCustomerPoints(HttpSession session, Model model) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Optional<Customer> custOpt = customerDAO.findByAccountId(user.getAccountId());
            custOpt.ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }
    }
}
