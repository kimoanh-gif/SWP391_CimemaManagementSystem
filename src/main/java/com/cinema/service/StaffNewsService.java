package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.model.NewsRequest;
import com.cinema.model.StaffNewsArticle;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class StaffNewsService {

    private final StaffNewsDAO staffNewsDAO;

    public StaffNewsService(StaffNewsDAO staffNewsDAO) {
        this.staffNewsDAO = staffNewsDAO;
    }

    public StaffNewsArticle create(int accountId, NewsRequest request) {
        int staffId = requireStaffId(accountId);
        String status = normalizeStatus(request.status());
        int newsId = staffNewsDAO.insert(staffId, request.title().trim(),
                cleanOptional(request.thumbnailUrl()), request.content().trim(), status);
        return getById(newsId);
    }

    public List<StaffNewsArticle> list(int accountId, String role) {
        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            return staffNewsDAO.findAll();
        }
        return staffNewsDAO.findAllByStaffId(requireStaffId(accountId));
    }

    public StaffNewsArticle update(int accountId, String role, int newsId, NewsRequest request) {
        StaffNewsArticle existing = getById(newsId);
        if (!"DRAFT".equalsIgnoreCase(existing.status()) && !"REJECTED".equalsIgnoreCase(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only Draft or Rejected news can be edited.");
        }
        boolean allowOtherOwners = "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
        Integer staffId = allowOtherOwners ? null : requireStaffId(accountId);
        if (!allowOtherOwners && !existing.createdByStaffId().equals(staffId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own news.");
        }
        String status = normalizeStatus(request.status());
        int updated = staffNewsDAO.update(newsId, staffId, allowOtherOwners, request.title().trim(),
                cleanOptional(request.thumbnailUrl()), request.content().trim(), status);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The news was changed and is no longer editable.");
        }
        return getById(newsId);
    }

    private StaffNewsArticle getById(int newsId) {
        return staffNewsDAO.findById(newsId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "News article was not found."));
    }

    private int requireStaffId(int accountId) {
        return staffNewsDAO.findActiveStaffId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active staff profile is required for this action."));
    }

    private static String normalizeStatus(String status) {
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private static String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
