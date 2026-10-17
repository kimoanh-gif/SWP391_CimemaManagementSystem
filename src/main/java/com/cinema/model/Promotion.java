package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Promotion for Screens 21 (Home) and 25 (Promotions & News).
 */
public class Promotion {

    private Integer promotionId;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String bannerUrl;
    private List<Voucher> vouchers = new ArrayList<>();

    public Promotion() {
    }

    public Promotion(Integer promotionId, String name, String description,
                     LocalDateTime startDate, LocalDateTime endDate, String status) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Integer getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Integer promotionId) {
        this.promotionId = promotionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBannerUrl() {
        if (bannerUrl != null && !bannerUrl.isBlank()) {
            return bannerUrl;
        }
        return "/images/cinema-login-hero.png";
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public List<Voucher> getVouchers() {
        return vouchers;
    }

    public void setVouchers(List<Voucher> vouchers) {
        this.vouchers = vouchers != null ? vouchers : new ArrayList<>();
    }

    // Helper methods
    public String getFormattedStartDate() {
        if (startDate == null) return "";
        return startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getFormattedEndDate() {
        if (endDate == null) return "Vô thời hạn";
        return endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getValidityRange() {
        return getFormattedStartDate() + " - " + getFormattedEndDate();
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public String getPrimaryVoucherCode() {
        if (vouchers != null && !vouchers.isEmpty()) {
            return vouchers.get(0).getCode();
        }
        return "CINEVERSE";
    }

    public String getDiscountDisplay() {
        if (vouchers != null && !vouchers.isEmpty()) {
            return vouchers.get(0).getDiscountDisplay();
        }
        return "Ưu Đãi Đặc Biệt";
    }
}
