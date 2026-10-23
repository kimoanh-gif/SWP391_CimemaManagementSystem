package com.cinema.controller;

import com.cinema.service.PromotionNewsService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Screen: Promotions &amp; News ( /promotions-news , /promotions , /news )
 * Use case: UC04 — View Promotions &amp; News
 *
 * - BR-PN-01 / BR-PN-02: only ACTIVE promotions inside their validity period.
 * - BR-PN-04: only PUBLISHED news whose publication time has arrived.
 * - BR-PN-06: when one content type is empty the other is still displayed.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class PromotionNewsListController {

    private final PromotionNewsService promotionNewsService;
    private final PublicPageModelHelper pageHelper;

    public PromotionNewsListController(PromotionNewsService promotionNewsService,
                                       PublicPageModelHelper pageHelper) {
        this.promotionNewsService = promotionNewsService;
        this.pageHelper = pageHelper;
    }

    @GetMapping("/promotions-news")
    public String listPromotionsAndNews(
            @RequestParam(name = "tab", required = false, defaultValue = "all") String tab,
            HttpSession session,
            Model model
    ) {
        pageHelper.populateHeader(session, model, "promotions-news");

        String activeTab = promotionNewsService.normalizeTab(tab);
        model.addAttribute("activeTab", activeTab);

        // Each content type is loaded independently so an empty/failed one never hides the other.
        model.addAttribute("promotions", "news".equals(activeTab) ? List.of()
                : pageHelper.loadSafely(model, promotionNewsService::getActivePromotions, List.of()));
        model.addAttribute("newsList", "promotions".equals(activeTab) ? List.of()
                : pageHelper.loadSafely(model, promotionNewsService::getPublishedNews, List.of()));

        return "customer/promotions-news/list";
    }

    /** Short link from the header / footer: opens the promotions tab. */
    @GetMapping("/promotions")
    public String promotionsTab() {
        return "redirect:/promotions-news?tab=promotions";
    }

    /** Short link from the header / footer: opens the news tab. */
    @GetMapping("/news")
    public String newsTab() {
        return "redirect:/promotions-news?tab=news";
    }
}
