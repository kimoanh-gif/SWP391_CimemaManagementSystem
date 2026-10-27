package com.cinema.controller;

import com.cinema.dao.CustomerBookingDAO;
import com.cinema.model.TicketVerification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TicketVerificationController {
    private final CustomerBookingDAO bookingDAO;

    public TicketVerificationController(CustomerBookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    @GetMapping("/ticket/verify/{qrCode}")
    public String verify(@PathVariable String qrCode, Model model) {
        bookingDAO.findTicketByQrCode(qrCode).ifPresentOrElse(ticket -> {
            model.addAttribute("ticket", ticket);
            model.addAttribute("maskedEmail", maskEmail(ticket.email()));
            model.addAttribute("maskedPhone", maskPhone(ticket.phone()));
        }, () -> model.addAttribute("invalidTicket", true));
        return "customer/booking/ticket-verification";
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return "Not provided";
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String visible = name.substring(0, Math.min(2, name.length()));
        return visible + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "Not provided";
        return phone.length() <= 4 ? "****" : "******" + phone.substring(phone.length() - 4);
    }
}
