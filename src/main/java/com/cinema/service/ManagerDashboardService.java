package com.cinema.service;

import com.cinema.model.FBItem;
import com.cinema.model.Genre;
import com.cinema.model.ManagerDashboardStats;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.model.TicketPricing;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service aggregating real database operational metrics for Screen 53: Manager Dashboard.
 */
@Service
public class ManagerDashboardService {

    private final MovieService movieService;
    private final GenreService genreService;
    private final RoomService roomService;
    private final ShowtimeService showtimeService;
    private final TicketPricingService pricingService;
    private final FBItemService fbItemService;

    public ManagerDashboardService(
            MovieService movieService,
            GenreService genreService,
            RoomService roomService,
            ShowtimeService showtimeService,
            TicketPricingService pricingService,
            FBItemService fbItemService
    ) {
        this.movieService = movieService;
        this.genreService = genreService;
        this.roomService = roomService;
        this.showtimeService = showtimeService;
        this.pricingService = pricingService;
        this.fbItemService = fbItemService;
    }

    public ManagerDashboardStats getDashboardStats() {
        ManagerDashboardStats stats = new ManagerDashboardStats();

        // 1. Movie stats
        List<Movie> allMovies = movieService.getAllMovies(null, null, null, null);
        stats.setTotalMovies(allMovies.size());
        stats.setNowShowingMovies((int) allMovies.stream()
                .filter(m -> "Now Showing".equalsIgnoreCase(m.getStatus()))
                .count());
        stats.setUpcomingMovies((int) allMovies.stream()
                .filter(m -> "Upcoming".equalsIgnoreCase(m.getStatus()))
                .count());

        // 2. Genre stats
        List<Genre> allGenres = genreService.getAllGenres(null, "ALL");
        stats.setTotalGenres(allGenres.size());

        // 3. Room stats
        List<Room> allRooms = roomService.getAll(null, null);
        stats.setTotalRooms(allRooms.size());
        stats.setActiveRooms((int) allRooms.stream()
                .filter(r -> "Ready".equalsIgnoreCase(r.getStatus()) || "Screening".equalsIgnoreCase(r.getStatus()))
                .count());

        // 4. Showtime stats (Today)
        LocalDate today = LocalDate.now();
        List<Showtime> todayShowtimes = showtimeService.getAllShowtimes(today, null, null, null);
        stats.setTodayShowtimesCount(todayShowtimes.size());

        // 5. Seat Type & Pricing stats
        List<SeatType> seatTypes = pricingService.getAllSeatTypes("ALL");
        stats.setTotalSeatTypes(seatTypes.size());

        List<TicketPricing> rules = pricingService.getAllPricingRules(null, "ALL");
        stats.setTotalPricingRules(rules.size());

        BigDecimal atp = pricingService.getAverageTicketPrice();
        stats.setAverageTicketPrice(atp != null ? atp : BigDecimal.ZERO);

        BigDecimal base2D = rules.stream()
                .filter(r -> "PRICE-WD-STD".equalsIgnoreCase(r.getPricingCode()))
                .map(TicketPricing::getBasePrice)
                .findFirst()
                .orElse(rules.isEmpty() ? new BigDecimal("85000") : rules.get(0).getBasePrice());
        stats.setBaseTicketPrice(base2D != null ? base2D : new BigDecimal("85000"));

        // 6. F&B stats
        List<FBItem> allItems = fbItemService.getAllItems(null, null, "ALL");
        stats.setTotalFBItems(allItems.size());
        stats.setActiveFBItems((int) allItems.stream()
                .filter(FBItem::isActive)
                .count());

        return stats;
    }

    public List<Showtime> getTodayShowtimes() {
        return showtimeService.getAllShowtimes(LocalDate.now(), null, null, null);
    }

    public List<Movie> getActiveMovies() {
        return movieService.getAllMovies(null, null, null, null).stream()
                .filter(m -> "Now Showing".equalsIgnoreCase(m.getStatus()) || "Upcoming".equalsIgnoreCase(m.getStatus()))
                .limit(8)
                .collect(Collectors.toList());
    }

    public List<Room> getRoomsSummary() {
        return roomService.getAll(null, null);
    }
}
