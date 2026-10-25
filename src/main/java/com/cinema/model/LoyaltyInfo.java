package com.cinema.model;

import java.util.List;

/**
 * Data of the Loyalty Information screen (UC18).
 */
public record LoyaltyInfo(
        String customerName,
        int availablePoints,
        int tierPoints,
        MembershipRank currentRank,
        MembershipRank nextRank,
        int pointsToNextRank,
        int progressPercent,
        List<String> rankBenefits,
        List<LoyaltyVoucher> availableVouchers
) {

    public String getCurrentRankName() {
        return rankName(currentRank, "Member");
    }

    public String getNextRankName() {
        return nextRank != null ? rankName(nextRank, "") : null;
    }

    public boolean isTopRank() {
        return nextRank == null;
    }

    private static String rankName(MembershipRank rank, String fallback) {
        if (rank == null) return fallback;
        if (rank.getDisplayName() != null && !rank.getDisplayName().isBlank()) return rank.getDisplayName();
        return rank.getName() != null ? rank.getName() : fallback;
    }

    /** A voucher the Customer can use with the current rank (BR-33). */
    public record LoyaltyVoucher(String code, String promotionName, String discountDisplay,
                                 String minBookingDisplay, String requiredRankName, String validityDisplay) {
    }
}
