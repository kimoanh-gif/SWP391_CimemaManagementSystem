package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Customer;
import com.cinema.model.Movie;
import com.cinema.model.PublicShowtime;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Screen 24: Showtime List (/showtimes)
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
@RequestMapping("/showtimes")
public class ShowtimePublicController {

    private final PublicShowtimeService showtimeService;
    private final MovieCatalogService movieService;
    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public ShowtimePublicController(PublicShowtimeService showtimeService,
                                    MovieCatalogService movieService,
                                    SessionService sessionService,
                                    CustomerDAO customerDAO) {
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    @GetMapping
    public String listShowtimes(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "movieId", required = false) Integer movieId,
            @RequestParam(name = "roomType", required = false, defaultValue = "ALL") String roomType,
            @RequestParam(name = "timeSlot", required = false, defaultValue = "ALL") String timeSlot,
            HttpSession session,
            Model model
    ) {
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();

        // 14 upcoming days for horizontal date selector
        List<LocalDate> upcomingDates = showtimeService.getUpcomingDates(14);
        model.addAttribute("upcomingDates", upcomingDates);
        model.addAttribute("selectedDate", selectedDate);

        // Fetch showtimes matching filter
        List<PublicShowtime> showtimes = showtimeService.getShowtimes(selectedDate, movieId, roomType, timeSlot);
        model.addAttribute("showtimes", showtimes);

        // Group showtimes by Movie Title/ID for intuitive cinema schedule layout
        Map<Integer, List<PublicShowtime>> groupedByMovieId = showtimes.stream()
                .collect(Collectors.groupingBy(PublicShowtime::getMovieId, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("groupedShowtimes", groupedByMovieId);

        // All active movies for dropdown filter
        List<Movie> allMovies = movieService.searchMovies(null, null, "ALL", "title");
        model.addAttribute("allMovies", allMovies);

        // Filter parameters for view state
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedRoomType", roomType);
        model.addAttribute("selectedTimeSlot", timeSlot);

        // Session user info
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Optional<Customer> custOpt = customerDAO.findByAccountId(user.getAccountId());
            custOpt.ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }

        model.addAttribute("activeNav", "showtimes");
        return "customer/showtimes/list";
    }
}
