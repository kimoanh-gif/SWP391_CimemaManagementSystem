package com.cinema.controller;

import com.cinema.model.ApiResponse;
import com.cinema.model.MemberLookupResult;
import com.cinema.service.MemberLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/staff/members")
public class StaffMemberLookupController {

    private final MemberLookupService memberLookupService;

    public StaffMemberLookupController(MemberLookupService memberLookupService) {
        this.memberLookupService = memberLookupService;
    }

    /**
     * Looks up an active member by {@code q} (phone or email, detected automatically),
     * or by an explicit {@code phone} / {@code email} parameter.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<MemberLookupResult>> search(@RequestParam(required = false) String q,
                                                                  @RequestParam(required = false) String phone,
                                                                  @RequestParam(required = false) String email) {
        MemberLookupResult member = q != null && !q.isBlank()
                ? memberLookupService.search(q)
                : memberLookupService.search(phone, email);
        return ResponseEntity.ok(ApiResponse.ok("Member found.", member));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleLookupError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(ex.getReason()));
    }
}
