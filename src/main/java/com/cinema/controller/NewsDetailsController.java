package com.cinema.controller;

import com.cinema.model.NewsArticle;
import com.cinema.service.PromotionNewsService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Screen: News Details ( /news/{id} ) — opened from Promotions &amp; News.
 * Use case: UC04 — View Promotions &amp; News
 *
 * BR-PN-04 / BR-PN-05: draft, pending or scheduled (future) articles are treated as not found.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class NewsDetailsController {

    static final int OTHER_NEWS_LIMIT = 3;
    static final String NOT_FOUND_MESSAGE = "This article could not be found or has not been published yet.";

    private final PromotionNewsService promotionNewsService;
    private final PublicPageModelHelper pageHelper;

    public NewsDetailsController(PromotionNewsService promotionNewsService,
                                 PublicPageModelHelper pageHelper) {
        this.promotionNewsService = promotionNewsService;
        this.pageHelper = pageHelper;
    }

    @GetMapping("/news/{id}")
    public String newsDetails(
            @PathVariable("id") int newsId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<NewsArticle> newsOpt;
        try {
            newsOpt = promotionNewsService.getNewsById(newsId);
        } catch (DataAccessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This article could not be loaded right now. Please try again.");
            return "redirect:/promotions-news?tab=news";
        }
        if (newsOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", NOT_FOUND_MESSAGE);
            return "redirect:/promotions-news?tab=news";
        }

        pageHelper.populateHeader(session, model, "promotions-news");
        model.addAttribute("news", newsOpt.get());
        model.addAttribute("otherNews", pageHelper.loadSafely(model,
                () -> promotionNewsService.getOtherNews(newsId, OTHER_NEWS_LIMIT), List.of()));
        return "customer/promotions-news/news-details";
    }
}
