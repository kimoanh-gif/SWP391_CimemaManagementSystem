package com.cinema.controller;

import com.cinema.model.BookingSummary;
import com.cinema.model.ShowtimeOption;
import com.cinema.service.CustomerBookingService;
import com.cinema.service.CustomerBookingService.BookingException;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/customer/booking")
public class CustomerBookingController {

    private static final String SESSION_SHOWTIME_ID = "customerBookingShowtimeId";
    private static final String SESSION_BOOKING_ID = "customerPendingBookingId";

    private final CustomerBookingService bookingService;
    private final SessionService sessionService;

    public CustomerBookingController(CustomerBookingService bookingService,
                                     SessionService sessionService) {
        this.bookingService = bookingService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String showMovieAndShowtimeSelection(
            @RequestParam(required = false) String date,
            HttpSession session,
            Model model
    ) {
        if (customerAccountId(session) == null) return "redirect:/login";

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
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", today);
        model.addAttribute("maxDate", today.plusDays(14));
        model.addAttribute("movies", bookingService.getMoviesAndShowtimes(selectedDate));
        model.addAttribute("activeStep", 1);
        return "customer/booking/showtimes";
    }

    @PostMapping("/showtime")
    public String selectShowtime(@RequestParam(required = false) String showtimeId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";

        try {
            int selectedShowtimeId = parsePositiveInteger(showtimeId);
            bookingService.getBookableShowtime(selectedShowtimeId);
            Integer oldBookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
            if (oldBookingId != null) {
                bookingService.cancelBooking(oldBookingId, accountId);
                session.removeAttribute(SESSION_BOOKING_ID);
            }
            session.setAttribute(SESSION_SHOWTIME_ID, selectedShowtimeId);
            return "redirect:/customer/booking/seats";
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking";
        }
    }

    @GetMapping("/select")
    public String selectShowtimeDirect(@RequestParam(name = "showtimeId", required = false) String showtimeId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) {
            return "redirect:/login?redirect=/customer/booking/select" + (showtimeId != null ? "?showtimeId=" + showtimeId : "");
        }
        return selectShowtime(showtimeId, session, redirectAttributes);
    }

    @GetMapping("/seats")
    public String showSeatSelection(HttpSession session, Model model,
                                    RedirectAttributes redirectAttributes) {
        if (customerAccountId(session) == null) return "redirect:/login";
        Integer showtimeId = (Integer) session.getAttribute(SESSION_SHOWTIME_ID);
        if (showtimeId == null) return "redirect:/customer/booking";

        Integer currentBookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (currentBookingId != null) {
            bookingService.cancelBooking(currentBookingId, customerAccountId(session));
            session.removeAttribute(SESSION_BOOKING_ID);
        }

        try {
            ShowtimeOption showtime = bookingService.getBookableShowtime(showtimeId);
            model.addAttribute("showtime", showtime);
            model.addAttribute("seats", bookingService.getSeats(showtimeId));
            model.addAttribute("maxSeats", CustomerBookingService.MAX_SEATS_PER_BOOKING);
            model.addAttribute("activeStep", 2);
            return "customer/booking/seats";
        } catch (BookingException exception) {
            clearBookingSession(session);
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking";
        }
    }

    @PostMapping("/seats")
    public String holdSeats(@RequestParam(required = false) List<String> seatIds,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer showtimeId = (Integer) session.getAttribute(SESSION_SHOWTIME_ID);
        if (showtimeId == null) return "redirect:/customer/booking";

        try {
            Integer previousBookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
            int bookingId = bookingService.holdSeats(
                    accountId, showtimeId, parseIntegerList(seatIds), previousBookingId);
            session.setAttribute(SESSION_BOOKING_ID, bookingId);
            return "redirect:/customer/booking/foods";
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/seats";
        }
    }

    @GetMapping("/foods")
    public String showFoodSelection(HttpSession session, Model model,
                                    RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/customer/booking/seats";

        try {
            BookingSummary summary = bookingService.getActiveBooking(bookingId, accountId);
            model.addAttribute("summary", summary);
            model.addAttribute("items", bookingService.getFoodItems(bookingId, accountId));
            model.addAttribute("activeStep", 3);
            return "customer/booking/foods";
        } catch (BookingException exception) {
            session.removeAttribute(SESSION_BOOKING_ID);
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/seats";
        }
    }

    @PostMapping("/foods")
    public String saveFoodSelection(@RequestParam(required = false) List<String> itemIds,
                                    @RequestParam(required = false) List<String> quantities,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/customer/booking/seats";

        try {
            bookingService.saveFoodSelections(
                    bookingId, accountId, parseIntegerList(itemIds), parseIntegerList(quantities));
            return "redirect:/customer/booking/promotions";
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/foods";
        }
    }

    @PostMapping("/foods/skip")
    public String skipFoodSelection(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/customer/booking/seats";
        try {
            bookingService.saveFoodSelections(bookingId, accountId, null, null);
            return "redirect:/customer/booking/promotions";
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/foods";
        }
    }

    @GetMapping("/promotions")
    public String showPromotionSelection(HttpSession session, Model model,
                                         RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/customer/booking/seats";
        try {
            model.addAttribute("summary", bookingService.getActiveBooking(bookingId, accountId));
            model.addAttribute("activeStep", 4);
            return "customer/booking/promotions";
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/seats";
        }
    }

    @PostMapping("/promotions")
    public String continueFromPromotions(@RequestParam(required = false) String voucherCode,
                                         HttpSession session) {
        if (customerAccountId(session) == null) return "redirect:/login";
        if (session.getAttribute(SESSION_BOOKING_ID) == null) return "redirect:/customer/booking/seats";
        session.setAttribute("customerBookingVoucherCode",
                voucherCode == null ? "" : voucherCode.trim().toUpperCase());
        return "redirect:/customer/booking/checkout";
    }

    @GetMapping("/checkout")
    public String showCheckout(HttpSession session, Model model,
                               RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/customer/booking/seats";
        try {
            model.addAttribute("summary", bookingService.getActiveBooking(bookingId, accountId));
            model.addAttribute("voucherCode", session.getAttribute("customerBookingVoucherCode"));
            model.addAttribute("activeStep", 5);
            return "customer/booking/checkout";
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/seats";
        }
    }

    @PostMapping("/checkout")
    public String submitCheckout(@RequestParam(defaultValue = "VNPAY") String paymentMethod,
                                 HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return "redirect:/customer/booking/seats";
        try {
            int paymentId = bookingService.startSimulatedPayment(bookingId, accountId, paymentMethod);
            session.setAttribute("customerPaymentId", paymentId);
            return "redirect:/customer/booking/payment/" + paymentId;
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/checkout";
        }
    }

    @GetMapping("/payment/{paymentId}")
    public String showPayment(@PathVariable int paymentId, HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        try {
            var payment = bookingService.getPayment(paymentId, accountId);
            model.addAttribute("payment", payment);
            model.addAttribute("summary", bookingService.getActiveBooking(payment.bookingId(), accountId));
            return "customer/booking/payment";
        } catch (BookingException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "customer/booking/payment";
        }
    }

    @PostMapping("/payment/{paymentId}/confirm")
    public String confirmPayment(@PathVariable int paymentId, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        try {
            int bookingId = bookingService.confirmSimulatedPayment(paymentId, accountId);
            clearBookingSession(session);
            session.removeAttribute("customerPaymentId");
            return "redirect:/customer/booking/success/" + bookingId;
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/payment/" + paymentId;
        }
    }

    @PostMapping("/payment/{paymentId}/regenerate")
    public String regeneratePayment(@PathVariable int paymentId, HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        try {
            int newPaymentId = bookingService.regeneratePayment(paymentId, accountId);
            session.setAttribute("customerPaymentId", newPaymentId);
            return "redirect:/customer/booking/payment/" + newPaymentId;
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking/seats";
        }
    }

    @GetMapping("/success/{bookingId}")
    public String showSuccess(@PathVariable int bookingId, HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        model.addAttribute("tickets", bookingService.getTickets(bookingId, accountId));
        model.addAttribute("bookingId", bookingId);
        return "customer/booking/success";
    }

    @GetMapping("/tickets/{bookingId}")
    public String showTickets(@PathVariable int bookingId, HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        model.addAttribute("tickets", bookingService.getTickets(bookingId, accountId));
        return "customer/booking/tickets";
    }

    @GetMapping("/history")
    public String showHistory(HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        model.addAttribute("bookings", bookingService.getBookingHistory(accountId));
        return "customer/booking/history";
    }

    @PostMapping("/cancel")
    public String cancelBooking(HttpSession session) {
        Integer accountId = customerAccountId(session);
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (accountId != null && bookingId != null) {
            bookingService.cancelBooking(bookingId, accountId);
        }
        clearBookingSession(session);
        return accountId == null ? "redirect:/login" : "redirect:/customer/booking";
    }

    private Integer customerAccountId(HttpSession session) {
        return sessionService.hasRole(session, "CUSTOMER")
                ? sessionService.currentAccountId(session)
                : null;
    }

    private int parsePositiveInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException | NullPointerException ignored) {
            // The caller turns malformed form values into a regular booking error.
        }
        throw new BookingException("The submitted selection is invalid. Please try again.");
    }

    private List<Integer> parseIntegerList(List<String> values) {
        if (values == null) return null;
        List<Integer> parsedValues = new ArrayList<>(values.size());
        try {
            for (String value : values) parsedValues.add(Integer.parseInt(value));
        } catch (NumberFormatException | NullPointerException exception) {
            throw new BookingException("The submitted selection contains an invalid value.");
        }
        return parsedValues;
    }

    private void clearBookingSession(HttpSession session) {
        session.removeAttribute(SESSION_SHOWTIME_ID);
        session.removeAttribute(SESSION_BOOKING_ID);
        session.removeAttribute("customerBookingVoucherCode");
        session.removeAttribute("customerPaymentId");
    }
}
