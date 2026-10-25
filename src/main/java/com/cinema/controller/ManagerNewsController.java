package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.model.ManagerNewsFilter;
import com.cinema.model.ManagerNewsItem;
import com.cinema.model.ManagerNewsStats;
import com.cinema.service.ManagerNewsService;
import com.cinema.service.ManagerNewsService.NewsReviewException;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Screen M07: News Management &amp; Approval (UC35 Review &amp; Approve News).
 * URLs:
 *  GET  /manager/news                     list + KPI + filters (+ ?view={id} opens the detail panel)
 *  POST /manager/news/{id}/approve        approve (publish now or schedule)
 *  POST /manager/news/{id}/reject         reject with reason
 *  POST /manager/news/{id}/archive        remove a published article from public display
 *  POST /manager/news/{id}/publish-now    publish a scheduled article immediately
 * Manager only (Permission Matrix: News Approve / Reject / Archive = Manager).
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Controller
@RequestMapping("/manager/news")
public class ManagerNewsController {

    private static final Logger log = LoggerFactory.getLogger(ManagerNewsController.class);

    private final ManagerNewsService newsService;
    private final SessionService sessionService;

    public ManagerNewsController(ManagerNewsService newsService, SessionService sessionService) {
        this.newsService = newsService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String viewNewsManagement(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String period,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size,
                                     @RequestParam(required = false) Integer view,
                                     HttpSession session, Model model) {
        if (!isManager(session)) return "redirect:/login";
        addShellAttributes(session, model);

        ManagerNewsFilter filter = ManagerNewsFilter.of(keyword, status, period, page, size);
        model.addAttribute("filter", filter);
        model.addAttribute("loadError", false);
        try {
            model.addAttribute("stats", newsService.getStats());
            model.addAttribute("newsPage", newsService.search(filter));
            model.addAttribute("history", newsService.getReviewHistory());
            if (view != null) {
                newsService.getNews(view).ifPresent(n -> model.addAttribute("selectedNews", n));
            }
        } catch (DataAccessException ex) {
            log.error("Cannot load news management data", ex);
            model.addAttribute("loadError", true);
            model.addAttribute("stats", ManagerNewsStats.empty());
            model.addAttribute("newsPage", new ManagerNewsService.NewsPage(List.of(), 0, 1, filter.size()));
            model.addAttribute("history", List.of());
        }
        model.addAttribute("maxReasonLength", ManagerNewsService.MAX_REASON_LENGTH);
        model.addAttribute("pageSizes", ManagerNewsFilter.PAGE_SIZES);
        return "manager/news";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") int newsId,
                          @RequestParam(defaultValue = "NOW") String publishMode,
                          @RequestParam(required = false) String publishAt,
                          @RequestParam(required = false) String returnQuery,
                          HttpSession session, RedirectAttributes redirect) {
        Integer managerId = managerAccountId(session);
        if (managerId == null) return "redirect:/login";
        try {
            LocalDateTime scheduled = "SCHEDULE".equalsIgnoreCase(publishMode) ? parseDateTime(publishAt) : null;
            ManagerNewsItem news = newsService.approve(newsId, managerId, scheduled);
            redirect.addFlashAttribute("successMessage", scheduled == null
                    ? "Article " + news.getCode() + " has been approved and published."
                    : "Article " + news.getCode() + " has been approved and will be published at the scheduled time.");
        } catch (NewsReviewException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return backToList(returnQuery);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") int newsId,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String returnQuery,
                         HttpSession session, RedirectAttributes redirect) {
        Integer managerId = managerAccountId(session);
        if (managerId == null) return "redirect:/login";
        try {
            ManagerNewsItem news = newsService.reject(newsId, managerId, reason);
            redirect.addFlashAttribute("successMessage", "Article " + news.getCode() + " has been rejected. The author can revise and resubmit it.");
        } catch (NewsReviewException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return backToList(returnQuery);
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable("id") int newsId,
                          @RequestParam(required = false) String returnQuery,
                          HttpSession session, RedirectAttributes redirect) {
        Integer managerId = managerAccountId(session);
        if (managerId == null) return "redirect:/login";
        try {
            ManagerNewsItem news = newsService.archive(newsId, managerId);
            redirect.addFlashAttribute("successMessage", "Article " + news.getCode() + " has been archived and removed from the App/Web.");
        } catch (NewsReviewException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return backToList(returnQuery);
    }

    @PostMapping("/{id}/publish-now")
    public String publishNow(@PathVariable("id") int newsId,
                             @RequestParam(required = false) String returnQuery,
                             HttpSession session, RedirectAttributes redirect) {
        Integer managerId = managerAccountId(session);
        if (managerId == null) return "redirect:/login";
        try {
            ManagerNewsItem news = newsService.publishNow(newsId, managerId);
            redirect.addFlashAttribute("successMessage", "Article " + news.getCode() + " is now published.");
        } catch (NewsReviewException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return backToList(returnQuery);
    }

    // ------------------------------------------------------------------ helpers

    private boolean isManager(HttpSession session) {
        return sessionService.hasRole(session, "MANAGER");
    }

    private Integer managerAccountId(HttpSession session) {
        return isManager(session) ? sessionService.currentAccountId(session) : null;
    }

    private void addShellAttributes(HttpSession session, Model model) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        model.addAttribute("userName", user != null ? user.getFullName() : null);
        model.addAttribute("userRole", sessionService.currentRole(session));
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new NewsReviewException("Please choose the scheduled publish time.");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new NewsReviewException("The scheduled publish time is invalid.");
        }
    }

    /** Keeps the current filters after an action; only a plain query string is accepted. */
    static String backToList(String returnQuery) {
        if (returnQuery != null && returnQuery.matches("[A-Za-z0-9=&%+._\\-#]*") && returnQuery.length() <= 300) {
            String q = returnQuery.replaceAll("(^|&)view=[^&]*", "");
            if (q.startsWith("&")) q = q.substring(1);
            return q.isEmpty() ? "redirect:/manager/news" : "redirect:/manager/news?" + q;
        }
        return "redirect:/manager/news";
    }
}
