package com.cinema.service;

import com.cinema.dao.PromotionDAO;
import com.cinema.model.Promotion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UC04 – View Promotions (public part).
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Service
public class PromotionPublicService {

    private final PromotionDAO promotionDAO;

    public PromotionPublicService(PromotionDAO promotionDAO) {
        this.promotionDAO = promotionDAO;
    }

    /** Active promotions inside their valid period (BR-PN-01, BR-PN-02). */
    public List<Promotion> getActivePromotions() {
        return promotionDAO.findActivePromotions();
    }

    /** Newest public promotions, used by the Home Page promotion section. */
    public List<Promotion> getLatestPromotions(int limit) {
        return promotionDAO.findLatestPromotions(limit);
    }

    /** Promotion detail – empty when the promotion is missing, inactive or expired (BR-PN-05). */
    public Optional<Promotion> getPublicPromotion(int promotionId) {
        if (promotionId <= 0) {
            return Optional.empty();
        }
        return promotionDAO.findPublicById(promotionId);
    }
}
