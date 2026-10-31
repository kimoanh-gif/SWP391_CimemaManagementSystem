package com.cinema.controller;

import com.cinema.service.CustomerHeaderModelService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class PolicyController {
    private final CustomerHeaderModelService headerModelService;

    public PolicyController(CustomerHeaderModelService headerModelService) {
        this.headerModelService = headerModelService;
    }

    @GetMapping("/policies")
    public String index(HttpSession session, Model model) {
        headerModelService.addHeaderAttributes(session, model, "policies");
        model.addAttribute("policies", policies());
        return "customer/policies/index";
    }

    @GetMapping("/policies/{slug}")
    public String detail(@PathVariable String slug, HttpSession session, Model model) {
        PolicyPage policy = policies().stream().filter(p -> p.slug().equals(slug)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        headerModelService.addHeaderAttributes(session, model, "policies");
        model.addAttribute("policy", policy);
        return "customer/policies/detail";
    }

    private static List<PolicyPage> policies() {
        return List.of(
                new PolicyPage("rewards", "CINEORA Rewards Policy", "How points, expiry dates, ranking cycles and member benefits work.", List.of(
                        new Section("Earning points", "Eligible, successfully paid bookings earn 1 point for every 10,000 VND, multiplied by the earning rate configured for the member's current rank. Failed, unpaid, expired or cancelled bookings do not earn points."),
                        new Section("Available Points", "Available Points may be used for supported rewards. Each earning batch expires at the end of the second month after it is earned. The Membership & Rewards page shows the nearest upcoming expiry."),
                        new Section("Tier Points and ranks", "Tier Points determine the member's rank using the active thresholds configured by CINEORA administrators. Members move up immediately after meeting a threshold."),
                        new Section("Ranking cycles", "Tier Points are collected in two cycles: January–June and July–December. At the start of a new cycle, Tier Points reset and the member returns to the lowest active rank until qualifying again."))),
                new PolicyPage("booking-tickets", "Booking, Payment & Ticket Policy", "Important rules for seat holds, payments, digital tickets and admission.", List.of(
                        new Section("Seat holds", "Selected seats are held only for the configured checkout period. If payment is not completed in time, the hold expires and the seats may become available to other customers."),
                        new Section("Payment and ticket issue", "A booking is confirmed and a digital ticket is issued only after successful payment for the exact final booking amount. Failed or incomplete payments do not create valid tickets."),
                        new Section("Admission", "Each QR ticket is valid for its booked movie, showtime, room and seat, and may be checked in only once. Age-restricted movies require customers to satisfy the applicable age rating."),
                        new Section("Cancellation and refunds", "The current online system does not support customer-requested cancellation, exchange or refund after successful payment. Please review the movie, showtime and seats carefully before paying."))),
                new PolicyPage("vouchers", "Voucher & Promotion Policy", "The conditions that determine whether a voucher can be applied.", List.of(
                        new Section("Eligibility", "A voucher must be active, within its validity period and below its total usage limit. Minimum booking value and membership-rank requirements may also apply."),
                        new Section("Usage limits", "Only one voucher can be applied to a booking. A voucher may also have a per-account usage limit."),
                        new Section("Final price", "The payable total consists of ticket and optional food-and-beverage subtotals, less a valid discount and plus applicable tax when enabled. Prices already recorded in a booking are not changed by later pricing updates."))),
                new PolicyPage("privacy", "Privacy & Account Policy", "How account access and customer information are protected in CINEORA.", List.of(
                        new Section("Account information", "Usernames, email addresses and phone numbers must be unique. New customer accounts require email verification, and only active accounts may sign in."),
                        new Section("Customer access", "Customers may access only their own profile, bookings, tickets and transaction history. Loyalty balances and membership ranks cannot be edited by customers."),
                        new Section("Transaction history", "Completed booking and payment records are retained for customer history, operational reporting and audit purposes.")))
        );
    }

    public record PolicyPage(String slug, String title, String summary, List<Section> sections) {}
    public record Section(String heading, String body) {}
}
