package com.cinema.service;

import com.cinema.dao.CustomerBookingDAO;
import com.cinema.model.BookingSummary;
import com.cinema.model.FoodBeverageOption;
import com.cinema.model.MovieBookingOption;
import com.cinema.model.SeatOption;
import com.cinema.model.ShowtimeOption;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.UUID;

@Service
public class CustomerBookingService {

    public static final int MAX_SEATS_PER_BOOKING = 10;
    public static final int HOLD_MINUTES = 10;

    private final CustomerBookingDAO bookingDAO;

    public CustomerBookingService(CustomerBookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    @Transactional
    public List<MovieBookingOption> getMoviesAndShowtimes(LocalDate date) {
        bookingDAO.expireOldHolds();
        List<MovieBookingOption> movies = bookingDAO.findMoviesWithShowtimes(date);
        movies.forEach(movie -> movie.setShowtimes(bookingDAO.findShowtimes(movie.getMovieId(), date)));
        return movies;
    }

    @Transactional
    public ShowtimeOption getBookableShowtime(int showtimeId) {
        bookingDAO.expireOldHolds();
        ShowtimeOption showtime = bookingDAO.findShowtime(showtimeId)
                .orElseThrow(() -> new BookingException("This showtime is no longer available."));
        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BookingException("This showtime has already started or ended.");
        }
        return showtime;
    }

    @Transactional
    public List<SeatOption> getSeats(int showtimeId) {
        return bookingDAO.findSeats(showtimeId);
    }

    @Transactional
    public int holdSeats(int accountId, int showtimeId, List<Integer> requestedSeatIds,
                         Integer previousBookingId) {
        ShowtimeOption showtime = getBookableShowtime(showtimeId);
        List<Integer> seatIds = requestedSeatIds == null
                ? List.of()
                : requestedSeatIds.stream().distinct().toList();

        if (seatIds.isEmpty()) {
            throw new BookingException("Select at least one seat to continue.");
        }
        if (seatIds.size() > MAX_SEATS_PER_BOOKING) {
            throw new BookingException("You can select up to " + MAX_SEATS_PER_BOOKING + " seats per booking.");
        }

        Map<Integer, SeatOption> availableSeats = new LinkedHashMap<>();
        for (SeatOption seat : bookingDAO.findSeats(showtimeId)) {
            if (seat.isAvailable() && seatIds.contains(seat.getSeatId())) {
                availableSeats.put(seat.getSeatId(), seat);
            }
        }
        if (availableSeats.size() != seatIds.size()) {
            throw new BookingException("One or more seats are no longer available. Please choose again.");
        }

        int customerId = bookingDAO.findCustomerId(accountId)
                .orElseThrow(() -> new BookingException("Your customer profile could not be found."));

        if (previousBookingId != null) {
            bookingDAO.releaseBooking(previousBookingId, accountId);
        }

        String bookingCode = generateBookingCode();
        LocalDateTime heldUntil = LocalDateTime.now().plusMinutes(HOLD_MINUTES);

        try {
            int bookingId = bookingDAO.createPendingBooking(customerId, showtime.getShowtimeId(), bookingCode);
            for (Integer seatId : seatIds) {
                bookingDAO.holdSeat(bookingId, showtimeId, availableSeats.get(seatId), heldUntil);
            }
            bookingDAO.recalculateBookingTotal(bookingId);
            return bookingId;
        } catch (DataIntegrityViolationException exception) {
            throw new BookingException("A seat was just reserved by another customer. Reload the seat map and try again.");
        }
    }

    @Transactional
    public BookingSummary getActiveBooking(int bookingId, int accountId) {
        bookingDAO.expireOldHolds();
        BookingSummary summary = bookingDAO.findBookingSummary(bookingId, accountId)
                .orElseThrow(() -> new BookingException("Your current booking could not be found."));
        if (!"PENDING".equalsIgnoreCase(summary.getStatus())
                || summary.getHeldUntil() == null
                || !summary.getHeldUntil().isAfter(LocalDateTime.now())) {
            throw new BookingException("Your seat hold has expired. Please select your seats again.");
        }
        return summary;
    }

    @Transactional
    public List<FoodBeverageOption> getFoodItems(int bookingId, int accountId) {
        getActiveBooking(bookingId, accountId);
        return bookingDAO.findFoodItems(bookingId);
    }

    @Transactional
    public void saveFoodSelections(int bookingId, int accountId,
                                   List<Integer> itemIds, List<Integer> quantities) {
        getActiveBooking(bookingId, accountId);
        if (itemIds == null && quantities == null) {
            bookingDAO.replaceFoodSelections(bookingId, List.of());
            bookingDAO.recalculateBookingTotal(bookingId);
            return;
        }
        if (itemIds == null || quantities == null || itemIds.size() != quantities.size()) {
            throw new BookingException("The food and drink selection is invalid.");
        }
        if (new HashSet<>(itemIds).size() != itemIds.size()) {
            throw new BookingException("The selected product list contains duplicates.");
        }

        Map<Integer, FoodBeverageOption> availableItems = new LinkedHashMap<>();
        bookingDAO.findFoodItems(bookingId)
                .forEach(item -> availableItems.put(item.getItemId(), item));

        List<FoodBeverageOption> selectedItems = new ArrayList<>();
        for (int index = 0; index < itemIds.size(); index++) {
            Integer itemId = itemIds.get(index);
            Integer quantity = quantities.get(index);
            FoodBeverageOption item = availableItems.get(itemId);
            if (item == null || quantity == null || quantity < 0 || quantity > 20) {
                throw new BookingException("Product quantities must be whole numbers between 0 and 20.");
            }
            item.setQuantity(quantity);
            selectedItems.add(item);
        }

        bookingDAO.replaceFoodSelections(bookingId, selectedItems);
        bookingDAO.recalculateBookingTotal(bookingId);
    }

    @Transactional
    public void cancelBooking(int bookingId, int accountId) {
        bookingDAO.releaseBooking(bookingId, accountId);
    }

    private String generateBookingCode() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BK" + time + random;
    }

    public static class BookingException extends RuntimeException {
        public BookingException(String message) {
            super(message);
        }
    }
}
