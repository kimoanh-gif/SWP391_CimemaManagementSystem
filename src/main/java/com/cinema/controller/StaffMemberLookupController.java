package com.cinema.controller;

import com.cinema.model.MemberLookupResult;
import com.cinema.service.MemberLookupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/members")
public class StaffMemberLookupController {

    private final MemberLookupService memberLookupService;

    public StaffMemberLookupController(MemberLookupService memberLookupService) {
        this.memberLookupService = memberLookupService;
    }

    @GetMapping("/search")
    public MemberLookupResult search(@RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String email) {
        return memberLookupService.search(phone, email);
    }
}
