package com.cinema.controller;

import com.cinema.dao.StaffOperationsDAO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Public page opened by scanning the QR on a ticket printed by Staff.
 *
 * Shows the same verification page as the customer ticket QR (template customer/booking/ticket-verification),
 * but looks the ticket up with {@link StaffOperationsDAO#findTicketVerification(String)}, which also finds
 * walk-in counter orders that have no customer account. The path is covered by the existing public
 * "/ticket/verify/**" rule, and does not clash with "/ticket/verify/{qrCode}" (single path segment).
 */
@Controller
public class StaffTicketVerificationController {

    private final StaffOperationsDAO staffOperationsDAO;

    public StaffTicketVerificationController(StaffOperationsDAO staffOperationsDAO) {
        this.staffOperationsDAO = staffOperationsDAO;
    }

    @GetMapping("/ticket/verify/counter/{qrCode}")
    public String verify(@PathVariable String qrCode, Model model) {
        staffOperationsDAO.findTicketVerification(qrCode).ifPresentOrElse(ticket -> {
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
        return name.substring(0, Math.min(2, name.length())) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "Not provided";
        return phone.length() <= 4 ? "****" : "******" + phone.substring(phone.length() - 4);
    }
}
