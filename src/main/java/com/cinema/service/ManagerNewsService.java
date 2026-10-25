package com.cinema.service;

import com.cinema.dao.ManagerNewsDAO;
import com.cinema.model.ManagerNewsFilter;
import com.cinema.model.ManagerNewsItem;
import com.cinema.model.ManagerNewsStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UC35 – Review &amp; Approve News (screen M07 News Management).
 *
 *  BR-45 news becomes Published only after a Manager approves it
 *  BR-46 only PENDING news can be approved / rejected
 *  BR-47 only Published news is visible to Guest / Customer (handled by NewsDAO)
 *  BR-48 rejected news keeps a reason so the Staff author can edit and resubmit
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Service
public class ManagerNewsService {

    public static final int MAX_REASON_LENGTH = 300;

    private final ManagerNewsDAO newsDAO;

    public ManagerNewsService(ManagerNewsDAO newsDAO) {
        this.newsDAO = newsDAO;
    }

    public static class NewsReviewException extends RuntimeException {
        public NewsReviewException(String message) {
            super(message);
        }
    }

    /** One page of the list plus totals. */
    public record NewsPage(List<ManagerNewsItem> items, int totalItems, int page, int size) {
        public int totalPages() {
            return Math.max(1, (int) Math.ceil(totalItems / (double) size));
        }
        public int fromItem() {
            return totalItems == 0 ? 0 : (page - 1) * size + 1;
        }
        public int toItem() {
            return Math.min(page * size, totalItems);
        }
    }

    public NewsPage search(ManagerNewsFilter filter) {
        int total = newsDAO.count(filter);
        ManagerNewsFilter safe = filter;
        int lastPage = Math.max(1, (int) Math.ceil(total / (double) filter.size()));
        if (filter.page() > lastPage) {
            safe = new ManagerNewsFilter(filter.keyword(), filter.status(), filter.period(), lastPage, filter.size());
        }
        return new NewsPage(newsDAO.search(safe), total, safe.page(), safe.size());
    }

    public ManagerNewsStats getStats() {
        ManagerNewsStats stats = newsDAO.stats();
        return stats != null ? stats : ManagerNewsStats.empty();
    }

    public Optional<ManagerNewsItem> getNews(int newsId) {
        return newsId > 0 ? newsDAO.findById(newsId) : Optional.empty();
    }

    public List<ManagerNewsItem> getReviewHistory() {
        return newsDAO.findReviewHistory(20);
    }

    /**
     * Approves a pending article.
     * publishAt == null → publish immediately; a future time → approved and scheduled.
     */
    @Transactional
    public ManagerNewsItem approve(int newsId, int managerAccountId, LocalDateTime publishAt) {
        ManagerNewsItem news = requirePending(newsId);
        if (publishAt != null && !publishAt.isAfter(LocalDateTime.now())) {
            throw new NewsReviewException("The scheduled publish time must be in the future.");
        }
        if (newsDAO.approve(newsId, managerAccountId, publishAt) != 1) {
            throw new NewsReviewException("This article was changed by someone else. Please reload the list.");
        }
        return news;
    }

    @Transactional
    public ManagerNewsItem reject(int newsId, int managerAccountId, String reason) {
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty()) {
            throw new NewsReviewException("Please enter a rejection reason before confirming.");
        }
        if (text.length() > MAX_REASON_LENGTH) {
            throw new NewsReviewException("The rejection reason must be at most " + MAX_REASON_LENGTH + " characters.");
        }
        ManagerNewsItem news = requirePending(newsId);
        if (newsDAO.reject(newsId, managerAccountId, text) != 1) {
            throw new NewsReviewException("This article was changed by someone else. Please reload the list.");
        }
        return news;
    }

    /** Removes a published (live or scheduled) article from public display. */
    @Transactional
    public ManagerNewsItem archive(int newsId, int managerAccountId) {
        ManagerNewsItem news = getNews(newsId)
                .orElseThrow(() -> new NewsReviewException("The article could not be found."));
        if (!"PUBLISHED".equals(news.getStatus())) {
            throw new NewsReviewException("Only published articles can be archived.");
        }
        if (newsDAO.archive(newsId, managerAccountId) != 1) {
            throw new NewsReviewException("This article was changed by someone else. Please reload the list.");
        }
        return news;
    }

    /** Publishes a scheduled (approved) article right now. */
    @Transactional
    public ManagerNewsItem publishNow(int newsId, int managerAccountId) {
        ManagerNewsItem news = getNews(newsId)
                .orElseThrow(() -> new NewsReviewException("The article could not be found."));
        if (!news.isScheduled()) {
            throw new NewsReviewException("Only approved articles waiting for their scheduled time can be published now.");
        }
        if (newsDAO.publishNow(newsId, managerAccountId) != 1) {
            throw new NewsReviewException("This article was changed by someone else. Please reload the list.");
        }
        return news;
    }

    private ManagerNewsItem requirePending(int newsId) {
        ManagerNewsItem news = getNews(newsId)
                .orElseThrow(() -> new NewsReviewException("The article could not be found."));
        if (!news.isPending()) {
            throw new NewsReviewException("Only articles waiting for approval (Pending) can be approved or rejected.");
        }
        return news;
    }
}
