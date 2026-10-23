package com.cinema.controller;

import com.cinema.service.CustomerAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for Customer Account Management (UC-36).
 * All endpoints are prefixed /api/admin/customers
 */
@RestController
@RequestMapping("/api/admin/customers")
public class CustomerAccountApiController {

    @Autowired
    private CustomerAccountService service;

    // GET  /api/admin/customers           → list all customers
    // GET  /api/admin/customers?search=x   → search customers
    // GET  /api/admin/customers/{id}      → get customer detail

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {

        // Validate page/size
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        var customers = service.searchCustomers(search, page, size);
        var total     = service.countCustomers(search);
        int totalPages = (int) Math.ceil((double) total / size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", customers,
                "pagination", Map.of(
                        "currentPage", page,
                        "pageSize",    size,
                        "totalItems",  total,
                        "totalPages",  totalPages
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable int id) {
        return service.getCustomerById(id)
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", c
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "code", "NOT_FOUND",
                        "message", "Customer account not found."
                )));
    }

    // POST /api/admin/customers            → A1: Create new customer
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        var result = service.createAccount(
                body.get("username"),
                body.get("email"),
                body.get("password"),
                body.get("fullName"),
                body.get("phone")
        );
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    // PUT /api/admin/customers/{id}        → A2: Update customer
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id,
                                   @RequestBody Map<String, String> body) {
        var result = service.updateAccount(
                id,
                body.get("username"),
                body.get("email"),
                body.get("fullName"),
                body.get("phone")
        );
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    // POST /api/admin/customers/{id}/suspend  → A3: Suspend
    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable int id) {
        var result = service.suspendAccount(id);
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    // POST /api/admin/customers/{id}/reactivate → A4: Reactivate
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable int id) {
        var result = service.reactivateAccount(id);
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    // POST /api/admin/customers/{id}/reset-password: email a new login password.
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable int id) {
        var result = service.resetPassword(id);
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    // GET /api/admin/customers/stats → Dashboard stats
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "total",     service.getTotalCount(),
                        "active",    service.getActiveCount(),
                        "suspended", service.getSuspendedCount(),
                        "pending",   service.getPendingCount()
                )
        ));
    }
}
