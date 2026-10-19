package com.cinema.service;

import com.cinema.model.FBItem;
import com.cinema.model.Genre;
import com.cinema.model.ManagerDashboardStats;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.model.TicketPricing;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    public ManagerDashboardService(
            MovieService movieService,
            GenreService genreService,
            RoomService roomService,
            ShowtimeService showtimeService,
            TicketPricingService pricingService,
            FBItemService fbItemService,
            JdbcTemplate jdbcTemplate
    ) {
        this.movieService = movieService;
        this.genreService = genreService;
        this.roomService = roomService;
        this.showtimeService = showtimeService;
        this.pricingService = pricingService;
        this.fbItemService = fbItemService;
        this.jdbcTemplate = jdbcTemplate;
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

        // 7. Transaction and Performance KPIs (P1-04)
        computeTransactionMetrics(stats);

        return stats;
    }

    private void computeTransactionMetrics(ManagerDashboardStats stats) {
        if (jdbcTemplate == null) {
            stats.setHasTransactionData(false);
            stats.setTotalRevenue(BigDecimal.ZERO);
            stats.setTicketRevenue(BigDecimal.ZERO);
            stats.setFbRevenue(BigDecimal.ZERO);
            stats.setSeatOccupancyRate(null);
            return;
        }

        try {
            // Check if Booking table exists
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Booking'", Integer.class);
            if (tableExists == null || tableExists == 0) {
                stats.setHasTransactionData(false);
                stats.setTotalRevenue(BigDecimal.ZERO);
                stats.setTicketRevenue(BigDecimal.ZERO);
                stats.setFbRevenue(BigDecimal.ZERO);
                stats.setSeatOccupancyRate(null);
                return;
            }

            Integer paidCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Booking WHERE Status IN ('Paid', 'Confirmed', 'Completed')", Integer.class);

            if (paidCount != null && paidCount > 0) {
                stats.setHasTransactionData(true);
                BigDecimal ticketRev = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(TotalAmount), 0) FROM Booking WHERE Status IN ('Paid', 'Confirmed', 'Completed')",
                        BigDecimal.class);
                stats.setTicketRevenue(ticketRev != null ? ticketRev : BigDecimal.ZERO);
                stats.setTotalRevenue(ticketRev != null ? ticketRev : BigDecimal.ZERO);

                // Booked tickets count
                Integer bookedCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM Ticket t JOIN Booking b ON t.BookingID = b.BookingID " +
                        "WHERE b.Status IN ('Paid', 'Confirmed', 'Completed')", Integer.class);
                stats.setTotalBookedTickets(bookedCount != null ? bookedCount : 0);

                // Capacity of scheduled showtimes
                Integer totalShowtimeCapacity = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(r.Capacity), 0) FROM Showtime s JOIN Room r ON s.RoomID = r.RoomID " +
                        "WHERE s.Status NOT IN ('Cancelled', 'Stopped')", Integer.class);

                if (totalShowtimeCapacity != null && totalShowtimeCapacity > 0 && bookedCount != null) {
                    double rate = ((double) bookedCount / totalShowtimeCapacity) * 100.0;
                    stats.setSeatOccupancyRate(Math.round(rate * 10.0) / 10.0);
                } else {
                    stats.setSeatOccupancyRate(0.0);
                }
            } else {
                stats.setHasTransactionData(false);
                stats.setTotalRevenue(BigDecimal.ZERO);
                stats.setTicketRevenue(BigDecimal.ZERO);
                stats.setFbRevenue(BigDecimal.ZERO);
                stats.setSeatOccupancyRate(null);
            }
        } catch (Exception ex) {
            // Fallback safely per AC04: Xử lý an toàn khi hệ thống mới triển khai chưa có dữ liệu giao dịch
            stats.setHasTransactionData(false);
            stats.setTotalRevenue(BigDecimal.ZERO);
            stats.setTicketRevenue(BigDecimal.ZERO);
            stats.setFbRevenue(BigDecimal.ZERO);
            stats.setSeatOccupancyRate(null);
        }
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
