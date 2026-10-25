package com.cinema.controller;

import com.cinema.model.Promotion;
import com.cinema.service.CustomerHeaderModelService;
import com.cinema.service.PromotionPublicService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Screen: Promotions &amp; News – Promotions tab (UC04).
 * URLs: /promotions, /promotions/{id}
 * Public – Guest and Customer, no login required.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class PromotionPublicController {

    private static final Logger log = LoggerFactory.getLogger(PromotionPublicController.class);
    static final String ACTIVE_NAV = "promotions-news";

    private final PromotionPublicService promotionService;
    private final CustomerHeaderModelService headerModelService;

    public PromotionPublicController(PromotionPublicService promotionService,
                                     CustomerHeaderModelService headerModelService) {
        this.promotionService = promotionService;
        this.headerModelService = headerModelService;
    }

    /**
     * Old combined URL kept so existing links (header, footer, bookmarks) still work.
     * It only redirects; the News tab is served by {@link NewsPublicController}.
     */
    @GetMapping("/promotions-news")
    public String legacyPromotionsNews(@RequestParam(name = "tab", required = false) String tab) {
        return "news".equalsIgnoreCase(tab) ? "redirect:/news" : "redirect:/promotions";
    }

    @GetMapping("/promotions")
    public String listPromotions(HttpSession session, Model model) {
        headerModelService.addHeaderAttributes(session, model, ACTIVE_NAV);
        model.addAttribute("activeTab", "promotions");
        model.addAttribute("loadError", false);
        try {
            List<Promotion> promotions = promotionService.getActivePromotions();
            model.addAttribute("promotions", promotions);
        } catch (DataAccessException ex) {
            // UC04 E1 – data retrieval error: show message + retry button.
            log.error("Cannot load promotions", ex);
            model.addAttribute("promotions", List.of());
            model.addAttribute("loadError", true);
        }
        return "customer/promotions-news/promotions";
    }

    @GetMapping("/promotions/{id}")
    public String promotionDetails(@PathVariable("id") int promotionId,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Optional<Promotion> promotion;
        try {
            promotion = promotionService.getPublicPromotion(promotionId);
        } catch (DataAccessException ex) {
            log.error("Cannot load promotion {}", promotionId, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to load the promotion right now. Please try again.");
            return "redirect:/promotions";
        }

        if (promotion.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This promotion does not exist or is no longer available.");
            return "redirect:/promotions";
        }

        headerModelService.addHeaderAttributes(session, model, ACTIVE_NAV);
        model.addAttribute("promotion", promotion.get());
        model.addAttribute("otherPromotions", promotionService.getLatestPromotions(4).stream()
                .filter(p -> !p.getPromotionId().equals(promotionId))
                .limit(3)
                .toList());
        return "customer/promotions-news/promotion-details";
    }
}
