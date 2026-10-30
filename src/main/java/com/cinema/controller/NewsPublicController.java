package com.cinema.controller;

import com.cinema.model.NewsArticle;
import com.cinema.service.CustomerHeaderModelService;
import com.cinema.service.NewsPublicService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Screen: Promotions &amp; News – News tab (UC04).
 * URLs: /news, /news/{id}
 * Public – Guest and Customer, no login required.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class NewsPublicController {

    private static final Logger log = LoggerFactory.getLogger(NewsPublicController.class);
    static final String ACTIVE_NAV = "promotions-news";

    private final NewsPublicService newsService;
    private final CustomerHeaderModelService headerModelService;

    public NewsPublicController(NewsPublicService newsService,
                                CustomerHeaderModelService headerModelService) {
        this.newsService = newsService;
        this.headerModelService = headerModelService;
    }

    @GetMapping("/news")
    public String listNews(HttpSession session, Model model) {
        return "redirect:/promotions#news-events";
    }

    @GetMapping("/news/{id}")
    public String newsDetails(@PathVariable("id") int newsId,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Optional<NewsArticle> article;
        try {
            article = newsService.getPublishedArticle(newsId);
        } catch (DataAccessException ex) {
            log.error("Cannot load news {}", newsId, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to load the article right now. Please try again.");
            return "redirect:/promotions#news-events";
        }

        if (article.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This article does not exist or has not been published.");
            return "redirect:/promotions#news-events";
        }

        headerModelService.addHeaderAttributes(session, model, ACTIVE_NAV);
        model.addAttribute("news", article.get());
        model.addAttribute("otherNews", newsService.getLatestNews(4).stream()
                .filter(n -> !n.getNewsId().equals(newsId))
                .limit(3)
                .toList());
        return "customer/promotions-news/news-details";
    }
}
