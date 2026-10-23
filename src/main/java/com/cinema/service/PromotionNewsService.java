package com.cinema.service;

import com.cinema.dao.NewsDAO;
import com.cinema.dao.PromotionDAO;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Business logic for the public Promotions & News screens (UC04).
 * Only ACTIVE promotions inside their validity period and PUBLISHED news whose
 * publication time has arrived are returned (BR-PN-01 … BR-PN-05).
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Service
public class PromotionNewsService {

    /** Tabs accepted on the Promotions & News screen. */
    public static final List<String> TABS = List.of("all", "promotions", "news");

    private final PromotionDAO promotionDAO;
    private final NewsDAO newsDAO;

    public PromotionNewsService(PromotionDAO promotionDAO, NewsDAO newsDAO) {
        this.promotionDAO = promotionDAO;
        this.newsDAO = newsDAO;
    }

    public List<Promotion> getActivePromotions() {
        return promotionDAO.findActivePromotions();
    }

    public List<Promotion> getLatestPromotions(int limit) {
        return promotionDAO.findLatestPromotions(limit);
    }

    /** Public promotion details: empty when the promotion is inactive, not started or expired. */
    public Optional<Promotion> getPromotionById(int promotionId) {
        if (promotionId <= 0) {
            return Optional.empty();
        }
        return promotionDAO.findPublicById(promotionId);
    }

    /** Other public promotions shown beside a promotion's details. */
    public List<Promotion> getOtherPromotions(int currentPromotionId, int limit) {
        return promotionDAO.findLatestPromotions(limit + 1).stream()
                .filter(p -> !Objects.equals(p.getPromotionId(), currentPromotionId))
                .limit(limit)
                .toList();
    }

    public List<NewsArticle> getPublishedNews() {
        return newsDAO.findAllPublished();
    }

    public List<NewsArticle> getLatestNews(int limit) {
        return newsDAO.findPublishedNews(limit);
    }

    /** Public article details: empty when the article is not published yet. */
    public Optional<NewsArticle> getNewsById(int newsId) {
        if (newsId <= 0) {
            return Optional.empty();
        }
        return newsDAO.findPublishedById(newsId);
    }

    /** Other published articles shown beside an article's details. */
    public List<NewsArticle> getOtherNews(int currentNewsId, int limit) {
        return newsDAO.findPublishedNews(limit + 1).stream()
                .filter(n -> !Objects.equals(n.getNewsId(), currentNewsId))
                .limit(limit)
                .toList();
    }

    public String normalizeTab(String tab) {
        if (tab == null) {
            return "all";
        }
        String value = tab.trim().toLowerCase();
        return TABS.contains(value) ? value : "all";
    }
}
