package com.cinema.controller;

import com.cinema.model.BookingSummary;
import com.cinema.model.CustomerTicket;
import com.cinema.model.ShowtimeOption;
import com.cinema.service.CustomerBookingService;
import com.cinema.service.CustomerBookingService.BookingException;
import com.cinema.service.SessionService;
import com.cinema.service.VnpayService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer/booking")
public class CustomerBookingController {

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    private static final String SESSION_SHOWTIME_ID = "customerBookingShowtimeId";
    private static final String SESSION_BOOKING_ID = "customerPendingBookingId";

    private final CustomerBookingService bookingService;
    private final SessionService sessionService;
    private final VnpayService vnpayService;

    public CustomerBookingController(CustomerBookingService bookingService,
                                     SessionService sessionService,
                                     VnpayService vnpayService) {
        this.bookingService = bookingService;
        this.sessionService = sessionService;
        this.vnpayService = vnpayService;
    }

    @ModelAttribute
    public void addBookingHeaderUser(HttpSession session, Model model) {
        model.addAttribute("currentUser", sessionService.getCurrentUser(session));
    }

    @GetMapping
    public String showMovieAndShowtimeSelection(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer movieId,
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
        var movies = bookingService.getMoviesAndShowtimes(selectedDate);
        if (movieId != null && movieId > 0) {
            movies = movies.stream()
                    .filter(movie -> movie.getMovieId() == movieId)
                    .toList();
            // A Home-page "Book Tickets" click has no date. Open the first
            // bookable day instead of showing an empty state for today.
            if ((date == null || date.isBlank()) && movies.isEmpty()) {
                for (int offset = 1; offset <= 14 && movies.isEmpty(); offset++) {
                    LocalDate candidateDate = today.plusDays(offset);
                    var candidateMovies = bookingService.getMoviesAndShowtimes(candidateDate);
                    movies = candidateMovies.stream()
                            .filter(movie -> movie.getMovieId() == movieId)
                            .toList();
                    if (!movies.isEmpty()) selectedDate = candidateDate;
                }
                model.addAttribute("selectedDate", selectedDate);
            }
            model.addAttribute("selectedMovieId", movieId);
        }
        model.addAttribute("movies", movies);
        model.addAttribute("activeStep", 1);
        return "customer/booking/showtimes";
    }

    @GetMapping("/change-showtime")
    public String changeShowtime(HttpSession session, RedirectAttributes redirectAttributes) {
        if (customerAccountId(session) == null) return "redirect:/login";
        Integer showtimeId = (Integer) session.getAttribute(SESSION_SHOWTIME_ID);
        if (showtimeId == null) return "redirect:/customer/booking";

        try {
            ShowtimeOption showtime = bookingService.getBookableShowtime(showtimeId);
            return "redirect:/customer/booking?movieId=" + showtime.getMovieId()
                    + "&date=" + showtime.getStartTime().toLocalDate();
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/booking";
        }
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

        try {
            ShowtimeOption showtime = bookingService.getBookableShowtime(showtimeId);
            Integer currentBookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
            List<Integer> selectedSeatIds = List.of();
            if (currentBookingId != null) {
                try {
                    bookingService.getActiveBooking(currentBookingId, customerAccountId(session));
                    selectedSeatIds = bookingService.getBookingSeatIds(currentBookingId, customerAccountId(session));
                } catch (BookingException exception) {
                    session.removeAttribute(SESSION_BOOKING_ID);
                    currentBookingId = null;
                }
            }
            model.addAttribute("showtime", showtime);
            model.addAttribute("seats", bookingService.getSeats(showtimeId, currentBookingId));
            model.addAttribute("selectedSeatIds", selectedSeatIds);
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
                                 HttpSession session, RedirectAttributes redirectAttributes,
                                 jakarta.servlet.http.HttpServletRequest request) {
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

    @GetMapping("/payment/vnpay/return")
    public String handleVnpayReturn(@RequestParam Map<String, String> parameters,
                                    HttpSession session, RedirectAttributes redirectAttributes) {
        if (!vnpayService.hasValidSignature(parameters)) {
            redirectAttributes.addFlashAttribute("errorMessage", "VNPay returned an invalid signature.");
            return "redirect:/customer/booking/checkout";
        }
        var payment = vnpayService.findPayment(parameters.get("vnp_TxnRef"));
        if (payment.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "The VNPay transaction could not be found.");
            return "redirect:/customer/booking/checkout";
        }
        if (!"00".equals(parameters.get("vnp_ResponseCode"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "VNPay payment was cancelled or unsuccessful.");
            return "redirect:/customer/booking/payment/" + payment.get().paymentId() + "?returned=true";
        }

        // VNPay may redirect the browser before its server-to-server IPN reaches a
        // local ngrok tunnel. The signed return contains the same transaction data,
        // so process it idempotently to make local/sandbox confirmation immediate.
        String resultCode = vnpayService.processIpn(parameters);
        var updatedPayment = vnpayService.findPayment(parameters.get("vnp_TxnRef"));
        if (updatedPayment.isPresent() && "PAID".equalsIgnoreCase(updatedPayment.get().status())) {
            clearBookingSession(session);
            session.removeAttribute("customerPaymentId");
            return "redirect:/customer/booking/success/" + updatedPayment.get().bookingId();
        }
        String message = "04".equals(resultCode)
                ? "VNPay returned an amount that does not match this booking."
                : "Payment was received but could not be confirmed. Please try again.";
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:/customer/booking/payment/" + payment.get().paymentId() + "?returned=true";
    }

    @GetMapping("/payment/{paymentId}")
    public String showPayment(@PathVariable int paymentId,
                              @RequestParam(defaultValue = "false") boolean returned,
                              HttpSession session, Model model,
                              jakarta.servlet.http.HttpServletRequest request) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        try {
            var payment = bookingService.getPayment(paymentId, accountId);
            if ("VNPAY".equalsIgnoreCase(payment.method())
                    && "PENDING".equalsIgnoreCase(payment.status()) && vnpayService.isReady()) {
                String gatewayUrl = vnpayService.createPaymentUrl(payment, request);
                model.addAttribute("gatewayUrl", gatewayUrl);
            }
            model.addAttribute("payment", payment);
            model.addAttribute("summary", bookingService.getActiveBooking(payment.bookingId(), accountId));
            return "customer/booking/payment";
        } catch (BookingException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "customer/booking/payment";
        }
    }

    @GetMapping("/payment/{paymentId}/status")
    @ResponseBody
    public Map<String, Object> paymentStatus(@PathVariable int paymentId, HttpSession session) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return Map.of("status", "UNAUTHORIZED");
        try {
            var payment = bookingService.getPayment(paymentId, accountId);
            return Map.of("status", payment.status(), "bookingId", payment.bookingId());
        } catch (BookingException exception) {
            return Map.of("status", "NOT_FOUND");
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
        model.addAttribute("ticketBaseUrl", publicBaseUrl.replaceAll("/+$", ""));
        return "customer/booking/tickets";
    }

    @GetMapping("/history")
    public String showHistory(HttpSession session, Model model) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        var bookings = bookingService.getBookingHistory(accountId);
        LocalDateTime now = LocalDateTime.now();
        long confirmedCount = bookings.stream()
                .filter(item -> "CONFIRMED".equalsIgnoreCase(item.bookingStatus()))
                .count();
        long upcomingCount = bookings.stream()
                .filter(item -> "CONFIRMED".equalsIgnoreCase(item.bookingStatus()))
                .filter(item -> item.startTime() != null && item.startTime().isAfter(now))
                .count();
        BigDecimal totalSpent = bookings.stream()
                .filter(item -> "PAID".equalsIgnoreCase(item.paymentStatus()))
                .map(item -> item.totalAmount() == null ? BigDecimal.ZERO : item.totalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Integer, List<CustomerTicket>> ticketsByBooking = new LinkedHashMap<>();
        bookings.stream()
                .filter(item -> "CONFIRMED".equalsIgnoreCase(item.bookingStatus()))
                .forEach(item -> ticketsByBooking.put(
                        item.bookingId(), bookingService.getTickets(item.bookingId(), accountId)));
        model.addAttribute("currentUser", sessionService.getCurrentUser(session));
        model.addAttribute("bookings", bookings);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("ticketsByBooking", ticketsByBooking);
        model.addAttribute("ticketBaseUrl", publicBaseUrl.replaceAll("/+$", ""));
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
