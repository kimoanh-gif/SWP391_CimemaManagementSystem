package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.ManagerNewsService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.function.Supplier;

/** M07 - News Management &amp; Approval for the Manager role (task #63). */
@Controller
public class ManagerNewsController {

    private static final String LIST_URL = "/manager/news";

    private final ManagerNewsService newsService;
    private final SessionService sessionService;

    public ManagerNewsController(ManagerNewsService newsService, SessionService sessionService) {
        this.newsService = newsService;
        this.sessionService = sessionService;
    }

    @GetMapping(LIST_URL)
    public String list(@RequestParam(name = "status", defaultValue = "PENDING") String status,
                       @RequestParam(name = "q", required = false) String keyword,
                       @RequestParam(name = "period", defaultValue = "all") String period,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @RequestParam(name = "size", defaultValue = "10") int size,
                       HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "MANAGER", "ADMIN")) return "redirect:/login";

        String safePeriod = ManagerNewsService.PERIODS.contains(period) ? period : "all";
        model.addAttribute("result", newsService.search(status, keyword, safePeriod, page, size));
        model.addAttribute("counts", newsService.getStatusCounts());
        model.addAttribute("history", newsService.getRecentHistory());
        model.addAttribute("status", status.toUpperCase());
        model.addAttribute("q", keyword == null ? "" : keyword.trim());
        model.addAttribute("period", safePeriod);
        model.addAttribute("pageSizes", ManagerNewsService.PAGE_SIZES);
        model.addAttribute("reasonMax", ManagerNewsService.REJECT_REASON_MAX);
        model.addAttribute("returnTo", UriComponentsBuilder.fromPath(LIST_URL)
                .queryParam("status", status.toUpperCase())
                .queryParamIfPresent("q", java.util.Optional.ofNullable(keyword).filter(k -> !k.isBlank()))
                .queryParam("period", safePeriod)
                .queryParam("page", page)
                .queryParam("size", size)
                .encode().build().toUriString());
        return "manager/news";
    }

    @PostMapping(LIST_URL + "/{id}/approve")
    public String approve(@PathVariable("id") int newsId,
                          @RequestParam(name = "mode", defaultValue = "publish") String mode,
                          @RequestParam(name = "returnTo", required = false) String returnTo,
                          HttpSession session, RedirectAttributes flash) {
        return run(session, flash, returnTo,
                () -> newsService.approve(newsId, accountId(session), !"approve".equals(mode)));
    }

    @PostMapping(LIST_URL + "/{id}/reject")
    public String reject(@PathVariable("id") int newsId,
                         @RequestParam(name = "reason", required = false) String reason,
                         @RequestParam(name = "returnTo", required = false) String returnTo,
                         HttpSession session, RedirectAttributes flash) {
        return run(session, flash, returnTo, () -> newsService.reject(newsId, accountId(session), reason));
    }

    @PostMapping(LIST_URL + "/{id}/publish")
    public String publish(@PathVariable("id") int newsId,
                          @RequestParam(name = "returnTo", required = false) String returnTo,
                          HttpSession session, RedirectAttributes flash) {
        return run(session, flash, returnTo, () -> newsService.publish(newsId, accountId(session)));
    }

    @PostMapping(LIST_URL + "/{id}/unpublish")
    public String unpublish(@PathVariable("id") int newsId,
                            @RequestParam(name = "returnTo", required = false) String returnTo,
                            HttpSession session, RedirectAttributes flash) {
        return run(session, flash, returnTo, () -> newsService.unpublish(newsId, accountId(session)));
    }

    @PostMapping(LIST_URL + "/{id}/reopen")
    public String reopen(@PathVariable("id") int newsId,
                         @RequestParam(name = "returnTo", required = false) String returnTo,
                         HttpSession session, RedirectAttributes flash) {
        return run(session, flash, returnTo, () -> newsService.reopen(newsId, accountId(session)));
    }

    private String run(HttpSession session, RedirectAttributes flash, String returnTo, Supplier<String> action) {
        if (!sessionService.hasRole(session, "MANAGER", "ADMIN")) return "redirect:/login";
        try {
            flash.addFlashAttribute("toast", action.get());
            flash.addFlashAttribute("toastType", "success");
        } catch (ManagerNewsService.NewsActionException e) {
            flash.addFlashAttribute("toast", e.getMessage());
            flash.addFlashAttribute("toastType", "error");
        }
        return "redirect:" + safeReturn(returnTo);
    }

    private int accountId(HttpSession session) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        if (user == null || user.getAccountId() == null) {
            throw new ManagerNewsService.NewsActionException("Your session has expired. Please sign in again.");
        }
        return user.getAccountId();
    }

    /** Only redirect back to the news list (keeps the current filters, prevents open redirects). */
    static String safeReturn(String returnTo) {
        String safe = SessionService.safeInternalRedirect(returnTo);
        if (safe == null) return LIST_URL;
        return safe.equals(LIST_URL) || safe.startsWith(LIST_URL + "?") ? safe : LIST_URL;
    }
}
