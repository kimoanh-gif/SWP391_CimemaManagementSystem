package com.cinema.controller;

import com.cinema.service.StaffAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for Staff & Manager Account Management.
 * All endpoints are prefixed /api/admin/staffs
 */
@RestController
@RequestMapping("/api/admin/staffs")
public class StaffAccountApiController {

    @Autowired
    private StaffAccountService service;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        var staffs = service.searchStaffs(search, page, size);
        var total = service.countStaffs(search);
        int totalPages = (int) Math.ceil((double) total / size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", staffs,
                "pagination", Map.of(
                        "currentPage", page,
                        "pageSize", size,
                        "totalItems", total,
                        "totalPages", totalPages
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable int id) {
        return service.getStaffById(id)
                .<ResponseEntity<?>>map(s -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", s
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "code", "NOT_FOUND",
                        "message", "Staff account not found."
                )));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        if (body.get("password") == null || !body.get("password").equals(body.get("confirmPassword"))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "code", "PASSWORD_MISMATCH", "message", "Passwords do not match."));
        }
        var result = service.createAccount(
                body.get("username"),
                body.get("email"),
                body.get("password"),
                body.get("fullName"),
                body.get("phone"),
                body.get("role")
        );
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id,
                                   @RequestBody Map<String, String> body) {
        var result = service.updateAccount(
                id,
                body.get("username"),
                body.get("email"),
                body.get("fullName"),
                body.get("phone"),
                body.get("role")
        );
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of("success", true, "message", result.getMessage()))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

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

    /**
     * A5 - Reset Password
     * Resets account password to a secure temporary password.
     * The password is emailed directly to the registered account address.
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable int id) {
        var result = service.resetPassword(id);
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.getMessage()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "total", service.getTotalCount(),
                        "admin", service.getAdminCount(),
                        "manager", service.getManagerCount(),
                        "staff", service.getStaffCount()
                )
        ));
    }
}
