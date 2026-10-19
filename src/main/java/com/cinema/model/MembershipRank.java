package com.cinema.model;

/**
 * Membership Rank model for loyalty program.
 */
public class MembershipRank {

    private Integer rankId;
    private String name;           // SILVER, GOLD, PLATINUM, DIAMOND
    private String displayName;   // Display name in UI
    private Integer minPoints;     // Minimum points required
    private Integer maxPoints;     // Maximum points (null for unlimited)
    private String benefits;       // JSON string of benefits
    private String discountPercent; // Discount percentage for this rank
    private Boolean isActive;

    public MembershipRank() {}

    public MembershipRank(Integer rankId, String name, String displayName, 
                         Integer minPoints, Integer maxPoints, 
                         String benefits, String discountPercent, Boolean isActive) {
        this.rankId = rankId;
        this.name = name;
        this.displayName = displayName;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.benefits = benefits;
        this.discountPercent = discountPercent;
        this.isActive = isActive;
    }

    // ==================== GETTERS & SETTERS ====================

    public Integer getRankId() { return rankId; }
    public void setRankId(Integer rankId) { this.rankId = rankId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getMinPoints() { return minPoints; }
    public void setMinPoints(Integer minPoints) { this.minPoints = minPoints; }

    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }

    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }

    public String getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(String discountPercent) { this.discountPercent = discountPercent; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // ==================== HELPER ====================

    public String getPointsRange() {
        if (maxPoints == null) {
            return minPoints + "+ points";
        }
        return minPoints + " - " + maxPoints + " points";
    }
}
