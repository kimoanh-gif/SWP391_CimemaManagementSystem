package com.cinema.service;

import com.cinema.dao.NewsDAO;
import com.cinema.dao.PromotionDAO;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PromotionNewsService {

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

    public Optional<Promotion> getPromotionById(int promotionId) {
        return promotionDAO.findById(promotionId);
    }

    public List<NewsArticle> getPublishedNews() {
        return newsDAO.findAllPublished();
    }

    public List<NewsArticle> getLatestNews(int limit) {
        return newsDAO.findPublishedNews(limit);
    }

    public Optional<NewsArticle> getNewsById(int newsId) {
        return newsDAO.findById(newsId);
    }
}
