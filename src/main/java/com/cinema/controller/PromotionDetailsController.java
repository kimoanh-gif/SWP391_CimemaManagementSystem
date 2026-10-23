package com.cinema.controller;

import com.cinema.model.Promotion;
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
 * Screen: Promotion Details ( /promotions/{id} ) — opened from Promotions &amp; News.
 * Use case: UC04 — View Promotions &amp; News
 *
 * BR-PN-05: an inactive, not-yet-started or expired promotion is treated as not found.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class PromotionDetailsController {

    static final int OTHER_PROMOTION_LIMIT = 3;
    static final String NOT_FOUND_MESSAGE = "This promotion could not be found or is no longer available.";

    private final PromotionNewsService promotionNewsService;
    private final PublicPageModelHelper pageHelper;

    public PromotionDetailsController(PromotionNewsService promotionNewsService,
                                      PublicPageModelHelper pageHelper) {
        this.promotionNewsService = promotionNewsService;
        this.pageHelper = pageHelper;
    }

    @GetMapping("/promotions/{id}")
    public String promotionDetails(
            @PathVariable("id") int promotionId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Promotion> promoOpt;
        try {
            promoOpt = promotionNewsService.getPromotionById(promotionId);
        } catch (DataAccessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Promotion details could not be loaded right now. Please try again.");
            return "redirect:/promotions-news?tab=promotions";
        }
        if (promoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", NOT_FOUND_MESSAGE);
            return "redirect:/promotions-news?tab=promotions";
        }

        pageHelper.populateHeader(session, model, "promotions-news");
        model.addAttribute("promotion", promoOpt.get());
        model.addAttribute("otherPromotions", pageHelper.loadSafely(model,
                () -> promotionNewsService.getOtherPromotions(promotionId, OTHER_PROMOTION_LIMIT), List.of()));
        return "customer/promotions-news/promotion-details";
    }
}
