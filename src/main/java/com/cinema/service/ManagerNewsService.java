package com.cinema.service;

import com.cinema.dao.ManagerNewsDAO;
import com.cinema.model.ManagerNewsItem;
import com.cinema.model.NewsReviewLogEntry;
import com.cinema.model.NewsStatusCounts;
import com.cinema.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Business rules for News Management &amp; Approval (M07, task #63).
 *
 * <pre>
 * DRAFT --(staff submits)--> PENDING --approve--> APPROVED --publish--> PUBLISHED
 *                               |   \--approve + publish now-------------^   |
 *                               |                 ^------unpublish-----------/
 *                               \--reject (reason required)--> REJECTED --reopen--> PENDING
 * </pre>
 */
@Service
public class ManagerNewsService {

    public static final int REJECT_REASON_MAX = 300;
    public static final List<Integer> PAGE_SIZES = List.of(5, 10, 20, 50);
    public static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "PUBLISHED", "DRAFT");
    public static final Set<String> PERIODS = Set.of("all", "week", "30d", "month");

    private final ManagerNewsDAO newsDAO;

    public ManagerNewsService(ManagerNewsDAO newsDAO) {
        this.newsDAO = newsDAO;
    }

    // ---------------------------------------------------------------- queries

    public PageResult<ManagerNewsItem> search(String status, String keyword, String period, int page, int size) {
        int pageSize = PAGE_SIZES.contains(size) ? size : 10;
        ManagerNewsDAO.Filter filter = new ManagerNewsDAO.Filter(
                normalizeStatus(status), trimToNull(keyword), periodStart(period, LocalDate.now()));
        int total = newsDAO.count(filter);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int safePage = Math.min(Math.max(1, page), totalPages);
        List<ManagerNewsItem> items = newsDAO.find(filter, (safePage - 1) * pageSize, pageSize);
        return new PageResult<>(items, safePage, pageSize, total);
    }

    public NewsStatusCounts getStatusCounts() {
        return new NewsStatusCounts(newsDAO.countByStatus(), newsDAO.countPendingSubmittedToday());
    }

    public List<NewsReviewLogEntry> getRecentHistory() {
        return newsDAO.findRecentLogs(30);
    }

    // ---------------------------------------------------------------- actions

    @Transactional
    public String approve(int newsId, int reviewerAccountId, boolean publishNow) {
        ManagerNewsItem news = requireStatus(newsId, "PENDING", "Only articles waiting for review can be approved.");
        if (!newsDAO.approve(newsId, reviewerAccountId, publishNow)) throw concurrentChange();
        String target = publishNow ? "PUBLISHED" : "APPROVED";
        newsDAO.insertLog(newsId, reviewerAccountId, "APPROVE", "PENDING", target, news.getTitle());
        return publishNow
                ? news.getCode() + " has been approved and published."
                : news.getCode() + " has been approved. It can be published later.";
    }

    @Transactional
    public String reject(int newsId, int reviewerAccountId, String reason) {
        String cleanReason = trimToNull(reason);
        if (cleanReason == null) {
            throw new NewsActionException("Please enter a rejection reason before confirming.");
        }
        if (cleanReason.length() > REJECT_REASON_MAX) {
            throw new NewsActionException("The rejection reason must be at most " + REJECT_REASON_MAX + " characters.");
        }
        ManagerNewsItem news = requireStatus(newsId, "PENDING", "Only articles waiting for review can be rejected.");
        if (!newsDAO.reject(newsId, reviewerAccountId, cleanReason)) throw concurrentChange();
        newsDAO.insertLog(newsId, reviewerAccountId, "REJECT", "PENDING", "REJECTED", cleanReason);
        return news.getCode() + " has been rejected. The reason was sent to the author.";
    }

    @Transactional
    public String publish(int newsId, int reviewerAccountId) {
        ManagerNewsItem news = requireStatus(newsId, "APPROVED", "Only approved articles can be published.");
        if (!newsDAO.publish(newsId, reviewerAccountId)) throw concurrentChange();
        newsDAO.insertLog(newsId, reviewerAccountId, "PUBLISH", "APPROVED", "PUBLISHED", news.getTitle());
        return news.getCode() + " is now published on the customer pages.";
    }

    @Transactional
    public String unpublish(int newsId, int reviewerAccountId) {
        ManagerNewsItem news = requireStatus(newsId, "PUBLISHED", "Only published articles can be unpublished.");
        if (!newsDAO.unpublish(newsId, reviewerAccountId)) throw concurrentChange();
        newsDAO.insertLog(newsId, reviewerAccountId, "UNPUBLISH", "PUBLISHED", "APPROVED", news.getTitle());
        return news.getCode() + " was unpublished and moved back to Approved.";
    }

    @Transactional
    public String reopen(int newsId, int reviewerAccountId) {
        ManagerNewsItem news = requireStatus(newsId, "REJECTED", "Only rejected articles can be sent back for review.");
        if (!newsDAO.reopen(newsId)) throw concurrentChange();
        newsDAO.insertLog(newsId, reviewerAccountId, "REOPEN", "REJECTED", "PENDING", news.getTitle());
        return news.getCode() + " is back in the review queue.";
    }

    // ---------------------------------------------------------------- helpers

    private ManagerNewsItem requireStatus(int newsId, String expected, String message) {
        ManagerNewsItem news = newsDAO.findById(newsId)
                .orElseThrow(() -> new NewsActionException("The article no longer exists."));
        if (!expected.equals(news.getStatus())) {
            throw new NewsActionException(message + " " + news.getCode() + " is currently "
                    + news.getStatusLabel().toLowerCase(Locale.ROOT) + ".");
        }
        return news;
    }

    private static NewsActionException concurrentChange() {
        return new NewsActionException("This article was just changed by someone else. Please reload and try again.");
    }

    static String normalizeStatus(String status) {
        if (status == null) return null;
        String upper = status.trim().toUpperCase(Locale.ROOT);
        return STATUSES.contains(upper) ? upper : null;
    }

    static LocalDateTime periodStart(String period, LocalDate today) {
        if (period == null) return null;
        return switch (period) {
            case "week" -> today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();
            case "30d" -> today.minusDays(30).atStartOfDay();
            case "month" -> today.withDayOfMonth(1).atStartOfDay();
            default -> null;
        };
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** A review action that is not allowed; the message is safe to show to the user. */
    public static class NewsActionException extends RuntimeException {
        public NewsActionException(String message) {
            super(message);
        }
    }
}
