package com.cinema.controller;

import com.cinema.dto.NewsRequest;
import com.cinema.model.StaffNewsArticle;
import com.cinema.service.SessionService;
import com.cinema.service.StaffNewsService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/staff/news")
public class StaffNewsApiController {

    private final StaffNewsService staffNewsService;
    private final SessionService sessionService;

    public StaffNewsApiController(StaffNewsService staffNewsService, SessionService sessionService) {
        this.staffNewsService = staffNewsService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<StaffNewsArticle> list(HttpSession session) {
        return staffNewsService.list(accountId(session), sessionService.currentRole(session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffNewsArticle create(@Valid @RequestBody NewsRequest request, HttpSession session) {
        return staffNewsService.create(accountId(session), request);
    }

    @PutMapping("/{id}")
    public StaffNewsArticle update(@PathVariable int id, @Valid @RequestBody NewsRequest request,
                                   HttpSession session) {
        return staffNewsService.update(accountId(session), sessionService.currentRole(session), id, request);
    }

    private int accountId(HttpSession session) {
        Integer accountId = sessionService.currentAccountId(session);
        if (accountId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return accountId;
    }
}
