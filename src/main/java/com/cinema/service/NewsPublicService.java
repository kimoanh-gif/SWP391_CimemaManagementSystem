package com.cinema.service;

import com.cinema.dao.NewsDAO;
import com.cinema.model.NewsArticle;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UC04 – View News (public part).
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Service
public class NewsPublicService {

    private final NewsDAO newsDAO;

    public NewsPublicService(NewsDAO newsDAO) {
        this.newsDAO = newsDAO;
    }

    /** Published news whose publication time has arrived (BR-PN-04, BR-47). */
    public List<NewsArticle> getPublishedNews() {
        return newsDAO.findAllPublished();
    }

    /** Newest published news, used by the Home Page and the "other news" block. */
    public List<NewsArticle> getLatestNews(int limit) {
        return newsDAO.findPublishedNews(limit);
    }

    /** News detail – empty when the article is missing or not published. */
    public Optional<NewsArticle> getPublishedArticle(int newsId) {
        if (newsId <= 0) {
            return Optional.empty();
        }
        return newsDAO.findPublishedById(newsId);
    }
}
