package com.cinema.service;

import com.cinema.dao.CustomerBookingDAO;
import com.cinema.dao.MemberLookupDAO;
import com.cinema.dao.StaffNewsDAO;
import com.cinema.model.BookingSummary;
import com.cinema.model.FoodBeverageOption;
import com.cinema.model.MovieBookingOption;
import com.cinema.model.SeatOption;
import com.cinema.model.ShowtimeOption;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StaffCounterSalesService {

    private final CustomerBookingDAO bookingDAO;
    private final CustomerBookingService customerBookingService;
    private final MemberLookupDAO memberLookupDAO;
    private final StaffNewsDAO staffNewsDAO;

    public StaffCounterSalesService(CustomerBookingDAO bookingDAO,
                                    CustomerBookingService customerBookingService,
                                    MemberLookupDAO memberLookupDAO,
                                    StaffNewsDAO staffNewsDAO) {
        this.bookingDAO = bookingDAO;
        this.customerBookingService = customerBookingService;
        this.memberLookupDAO = memberLookupDAO;
        this.staffNewsDAO = staffNewsDAO;
    }

    public List<MovieBookingOption> movies(LocalDate date) {
        return customerBookingService.getMoviesAndShowtimes(date);
    }

    public ShowtimeOption showtime(int showtimeId) {
        return customerBookingService.getBookableShowtime(showtimeId);
    }

    public List<SeatOption> seats(int showtimeId) {
        return customerBookingService.getSeats(showtimeId);
    }

    /** Counter orders are recorded against a Staff profile; manager/admin accounts may not have one. */
    public boolean canSell(int accountId) {
        return staffNewsDAO.findActiveStaffId(accountId).isPresent();
    }

    @Transactional
    public int createPendingCounterBooking(int accountId, int showtimeId, List<Integer> requestedSeatIds,
                                           String memberIdentifier) {
        int staffId = staffNewsDAO.findActiveStaffId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active staff profile is required to sell tickets."));
        ShowtimeOption showtime = customerBookingService.getBookableShowtime(showtimeId);
        List<Integer> seatIds = requestedSeatIds == null ? List.of()
                : requestedSeatIds.stream().distinct().toList();
        if (seatIds.isEmpty() || seatIds.size() > CustomerBookingService.MAX_SEATS_PER_BOOKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Select between 1 and " + CustomerBookingService.MAX_SEATS_PER_BOOKING + " seats.");
        }

        Map<Integer, SeatOption> availableSeats = new LinkedHashMap<>();
        for (SeatOption seat : bookingDAO.findSeats(showtimeId)) {
            if (seat.isAvailable() && seatIds.contains(seat.getSeatId())) {
                availableSeats.put(seat.getSeatId(), seat);
            }
        }
        if (availableSeats.size() != seatIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "One or more selected seats are no longer available.");
        }

        Integer customerId = null;
        if (memberIdentifier != null && !memberIdentifier.isBlank()) {
            String identifier = memberIdentifier.trim();
            if (!identifier.contains("@")) identifier = MemberLookupService.normalizePhone(identifier);
            customerId = memberLookupDAO.findCustomerIdByPhoneOrEmail(identifier)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No active member matched the phone number or email."));
        }

        String code = "BK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LocalDateTime heldUntil = LocalDateTime.now().plusMinutes(CustomerBookingService.HOLD_MINUTES);
        try {
            int bookingId = bookingDAO.createPendingCounterBooking(customerId, staffId,
                    showtime.getShowtimeId(), code);
            for (int seatId : seatIds) {
                bookingDAO.holdSeat(bookingId, showtimeId, availableSeats.get(seatId), heldUntil);
            }
            bookingDAO.recalculateBookingTotal(bookingId);
            return bookingId;
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seat was just reserved by another customer. Reload the seat map and try again.", exception);
        }
    }

    @Transactional
    public BookingSummary activeBooking(int accountId, int bookingId) {
        bookingDAO.expireOldHolds();
        int staffId = staffNewsDAO.findActiveStaffId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active staff profile is required."));
        BookingSummary summary = bookingDAO.findCounterBookingSummary(bookingId, staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Counter order was not found."));
        if (!"PENDING".equalsIgnoreCase(summary.getStatus()) || summary.getHeldUntil() == null
                || !summary.getHeldUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The seat hold has expired or the order is no longer pending.");
        }
        return summary;
    }

    @Transactional
    public List<FoodBeverageOption> foodItems(int accountId, int bookingId) {
        activeBooking(accountId, bookingId);
        return bookingDAO.findFoodItems(bookingId);
    }

    @Transactional
    public void saveFoodSelections(int accountId, int bookingId,
                                   List<Integer> itemIds, List<Integer> quantities) {
        activeBooking(accountId, bookingId);
        if (itemIds == null && quantities == null) {
            bookingDAO.replaceFoodSelections(bookingId, List.of());
            bookingDAO.recalculateBookingTotal(bookingId);
            return;
        }
        if (itemIds == null || quantities == null || itemIds.size() != quantities.size()
                || new HashSet<>(itemIds).size() != itemIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The food selection is invalid.");
        }
        Map<Integer, FoodBeverageOption> available = new LinkedHashMap<>();
        bookingDAO.findFoodItems(bookingId).forEach(item -> available.put(item.getItemId(), item));
        List<FoodBeverageOption> selected = new ArrayList<>();
        for (int index = 0; index < itemIds.size(); index++) {
            FoodBeverageOption item = available.get(itemIds.get(index));
            Integer quantity = quantities.get(index);
            if (item == null || quantity == null || quantity < 0 || quantity > 20) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Food quantities must be whole numbers from 0 to 20.");
            }
            item.setQuantity(quantity);
            selected.add(item);
        }
        bookingDAO.replaceFoodSelections(bookingId, selected);
        bookingDAO.recalculateBookingTotal(bookingId);
    }

    @Transactional
    public void cancel(int accountId, int bookingId) {
        int staffId = staffNewsDAO.findActiveStaffId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active staff profile is required."));
        activeBooking(accountId, bookingId);
        bookingDAO.releaseCounterBooking(bookingId, staffId);
    }
}
