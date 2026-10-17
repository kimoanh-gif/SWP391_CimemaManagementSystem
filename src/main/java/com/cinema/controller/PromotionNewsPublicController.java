package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Customer;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import com.cinema.service.PromotionNewsService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Screen 25: Promotions & News (/promotions-news, /promotions/{id}, /news/{id})
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class PromotionNewsPublicController {

    private final PromotionNewsService promotionNewsService;
    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public PromotionNewsPublicController(PromotionNewsService promotionNewsService,
                                         SessionService sessionService,
                                         CustomerDAO customerDAO) {
        this.promotionNewsService = promotionNewsService;
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    @GetMapping({"/promotions-news", "/promotions", "/news"})
    public String listPromotionsAndNews(
            @RequestParam(name = "tab", required = false, defaultValue = "all") String tab,
            HttpSession session,
            Model model
    ) {
        List<Promotion> promotions = promotionNewsService.getActivePromotions();
        List<NewsArticle> newsList = promotionNewsService.getPublishedNews();

        model.addAttribute("promotions", promotions);
        model.addAttribute("newsList", newsList);
        model.addAttribute("activeTab", tab);

        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Optional<Customer> custOpt = customerDAO.findByAccountId(user.getAccountId());
            custOpt.ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }

        model.addAttribute("activeNav", "promotions-news");
        return "customer/promotions-news/list";
    }

    @GetMapping("/promotions/{id}")
    public String promotionDetails(
            @PathVariable("id") int promotionId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Promotion> promoOpt = promotionNewsService.getPromotionById(promotionId);
        if (promoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy chương trình khuyến mãi.");
            return "redirect:/promotions-news?tab=promotions";
        }

        model.addAttribute("promotion", promoOpt.get());
        model.addAttribute("otherPromotions", promotionNewsService.getLatestPromotions(3));

        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Optional<Customer> custOpt = customerDAO.findByAccountId(user.getAccountId());
            custOpt.ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }

        model.addAttribute("activeNav", "promotions-news");
        return "customer/promotions-news/promotion-details";
    }

    @GetMapping("/news/{id}")
    public String newsDetails(
            @PathVariable("id") int newsId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<NewsArticle> newsOpt = promotionNewsService.getNewsById(newsId);
        if (newsOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết tin tức.");
            return "redirect:/promotions-news?tab=news";
        }

        model.addAttribute("news", newsOpt.get());
        model.addAttribute("otherNews", promotionNewsService.getLatestNews(3));

        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            Optional<Customer> custOpt = customerDAO.findByAccountId(user.getAccountId());
            custOpt.ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }

        model.addAttribute("activeNav", "promotions-news");
        return "customer/promotions-news/news-details";
    }
}
