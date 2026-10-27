package com.cinema.controller;

import com.cinema.model.BookingSummary;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.StaffBookingDetail;
import com.cinema.model.StaffBookingPage;
import com.cinema.model.StaffNewsArticle;
import com.cinema.service.CustomerBookingService;
import com.cinema.service.SessionService;
import com.cinema.service.StaffCounterSalesService;
import com.cinema.service.StaffNewsService;
import com.cinema.service.StaffOperationsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/staff")
public class StaffWorkspaceController {

    private static final String SESSION_SHOWTIME_ID = "staffCounterShowtimeId";
    private static final String SESSION_BOOKING_ID = "staffCounterBookingId";

    private final SessionService sessionService;
    private final StaffNewsService staffNewsService;
    private final StaffCounterSalesService counterSalesService;
    private final StaffOperationsService operationsService;

    public StaffWorkspaceController(SessionService sessionService, StaffNewsService staffNewsService,
                                    StaffCounterSalesService counterSalesService,
                                    StaffOperationsService operationsService) {
        this.sessionService = sessionService;
        this.staffNewsService = staffNewsService;
        this.counterSalesService = counterSalesService;
        this.operationsService = operationsService;
    }

    @GetMapping("/news")
    public String news(HttpSession session, Model model) {
        Integer accountId = staffAccountId(session);
        if (accountId == null) return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        model.addAttribute("articles", staffNewsService.list(accountId, sessionService.currentRole(session)));
        model.addAttribute("newsForm", new StaffNewsArticle(null, null, "", "", "",
                "DRAFT", null, null, null));
        return "staff/news";
    }

    @GetMapping({"/sales", "/sales/"})
    public String sales(@RequestParam(required = false) String date, HttpSession session, Model model) {
        if (staffAccountId(session) == null) return "redirect:/login";
        LocalDate today = LocalDate.now();
        LocalDate selectedDate = today;
        if (date != null && !date.isBlank()) {
            try {
                selectedDate = LocalDate.parse(date);
            } catch (DateTimeParseException exception) {
                model.addAttribute("errorMessage", "Please select a valid show date.");
            }
        }
        if (selectedDate.isBefore(today)) selectedDate = today;
        if (selectedDate.isAfter(today.plusDays(14))) selectedDate = today.plusDays(14);
        model.addAttribute("user", sessionService.getCurrentUser(session));
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", today);
        model.addAttribute("maxDate", today.plusDays(14));
        model.addAttribute("movies", counterSalesService.movies(selectedDate));
        return "staff/sales-showtimes";
    }

    @PostMapping("/sales/showtime")
    public String selectShowtime(@RequestParam int showtimeId, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (staffAccountId(session) == null) return "redirect:/login";
        try {
            counterSalesService.showtime(showtimeId);
            session.setAttribute(SESSION_SHOWTIME_ID, showtimeId);
            return "redirect:/staff/sales/seats";
        } catch (CustomerBookingService.BookingException | ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            return "redirect:/staff/sales";
        }
    }

    @GetMapping("/sales/seats")
    public String seats(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (staffAccountId(session) == null) return "redirect:/login";
        Integer showtimeId = (Integer) session.getAttribute(SESSION_SHOWTIME_ID);
        if (showtimeId == null) return "redirect:/staff/sales";
        try {
            model.addAttribute("user", sessionService.getCurrentUser(session));
            model.addAttribute("showtime", counterSalesService.showtime(showtimeId));
            model.addAttribute("seats", counterSalesService.seats(showtimeId));
            model.addAttribute("maxSeats", 10);
            return "staff/sales-seats";
        } catch (CustomerBookingService.BookingException | ResponseStatusException exception) {
            session.removeAttribute(SESSION_SHOWTIME_ID);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            return "redirect:/staff/sales";
        }
    }

    @PostMapping("/sales/seats")
    public String holdSeats(@RequestParam(required = false) List<String> seatIds,
                            @RequestParam(required = false) String memberIdentifier,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = staffAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer showtimeId = (Integer) session.getAttribute(SESSION_SHOWTIME_ID);
        if (showtimeId == null) return "redirect:/staff/sales";
        try {
            int bookingId = counterSalesService.createPendingCounterBooking(
                    accountId, showtimeId, parseIntegerList(seatIds), memberIdentifier);
            session.setAttribute(SESSION_BOOKING_ID, bookingId);
            return "redirect:/staff/sales/foods";
        } catch (CustomerBookingService.BookingException | ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            return "redirect:/staff/sales/seats";
        }
    }

    @GetMapping("/sales/foods")
    public String foods(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Integer accountId = staffAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/staff/sales/seats";
        try {
            BookingSummary summary = counterSalesService.activeBooking(accountId, bookingId);
            model.addAttribute("user", sessionService.getCurrentUser(session));
            model.addAttribute("summary", summary);
            model.addAttribute("items", counterSalesService.foodItems(accountId, bookingId));
            return "staff/sales-foods";
        } catch (ResponseStatusException exception) {
            session.removeAttribute(SESSION_BOOKING_ID);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            return "redirect:/staff/sales/seats";
        }
    }

    @PostMapping("/sales/foods")
    public String saveFoods(@RequestParam(required = false) List<String> itemIds,
                            @RequestParam(required = false) List<String> quantities,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = staffAccountId(session);
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (accountId == null) return "redirect:/login";
        if (bookingId == null) return "redirect:/staff/sales";
        try {
            counterSalesService.saveFoodSelections(accountId, bookingId,
                    parseIntegerList(itemIds), parseIntegerList(quantities));
            session.removeAttribute(SESSION_BOOKING_ID);
            session.removeAttribute(SESSION_SHOWTIME_ID);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Counter order created with Pending status.");
            AuthenticatedUser user = sessionService.getCurrentUser(session);
            return user != null && user.hasPermission("booking:read")
                    ? "redirect:/staff/bookings/" + bookingId : "redirect:/staff/sales";
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage(exception));
            return "redirect:/staff/sales/foods";
        }
    }

    @PostMapping("/sales/cancel")
    public String cancelSales(HttpSession session) {
        Integer accountId = staffAccountId(session);
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (accountId != null && bookingId != null) counterSalesService.cancel(accountId, bookingId);
        session.removeAttribute(SESSION_BOOKING_ID);
        session.removeAttribute(SESSION_SHOWTIME_ID);
        return accountId == null ? "redirect:/login" : "redirect:/staff/sales";
    }

    @GetMapping("/bookings")
    public String bookings(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           HttpSession session, Model model) {
        Integer accountId = staffAccountId(session);
        if (accountId == null) return "redirect:/login";
        StaffBookingPage bookings = operationsService.bookings(
                accountId, sessionService.currentRole(session), page, q, status);
        model.addAttribute("user", sessionService.getCurrentUser(session));
        model.addAttribute("bookingPage", bookings);
        return "staff/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable int id, HttpSession session, Model model) {
        Integer accountId = staffAccountId(session);
        if (accountId == null) return "redirect:/login";
        StaffBookingDetail detail = operationsService.bookingDetail(
                accountId, sessionService.currentRole(session), id);
        model.addAttribute("user", sessionService.getCurrentUser(session));
        model.addAttribute("booking", detail);
        return "staff/booking-detail";
    }

    @GetMapping("/check-in")
    public String checkIn(HttpSession session, Model model) {
        if (staffAccountId(session) == null) return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        return "staff/check-in";
    }

    @PostMapping("/check-in")
    public String checkIn(@RequestParam String bookingCode, HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (staffAccountId(session) == null) return "redirect:/login";
        try {
            int checkedIn = operationsService.checkIn(bookingCode);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully checked in " + checkedIn + " ticket(s) for booking "
                            + bookingCode.trim() + ".");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
        }
        return "redirect:/staff/check-in";
    }

    private Integer staffAccountId(HttpSession session) {
        return sessionService.hasRole(session, "STAFF", "MANAGER", "ADMIN")
                ? sessionService.currentAccountId(session) : null;
    }

    private String errorMessage(RuntimeException exception) {
        return exception instanceof ResponseStatusException responseStatusException
                ? responseStatusException.getReason() : exception.getMessage();
    }

    private List<Integer> parseIntegerList(List<String> values) {
        if (values == null) return null;
        List<Integer> parsed = new ArrayList<>(values.size());
        try {
            for (String value : values) parsed.add(Integer.parseInt(value));
        } catch (NumberFormatException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The submitted list contains an invalid value.");
        }
        return parsed;
    }
}
