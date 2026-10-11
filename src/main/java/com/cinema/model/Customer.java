package com.cinema.model;

import java.time.LocalDate;

public class Customer {
    private Integer customerId;
    private Integer accountId;
    private Integer rankId;
    private LocalDate dateOfBirth;
    private String  address;
    private Integer availablePoints;
    private Integer tierPoints;
    private Boolean receivePromotionEmail;

    public Customer() {}

    public Customer(Integer customerId, Integer accountId, Integer rankId, LocalDate dateOfBirth,
                    String address, Integer availablePoints, Integer tierPoints,
                    Boolean receivePromotionEmail) {
        this.customerId = customerId;
        this.accountId = accountId;
        this.rankId = rankId;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.availablePoints = availablePoints;
        this.tierPoints = tierPoints;
        this.receivePromotionEmail = receivePromotionEmail;
    }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public Integer getRankId() { return rankId; }
    public void setRankId(Integer rankId) { this.rankId = rankId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Integer availablePoints) { this.availablePoints = availablePoints; }

    public Integer getTierPoints() { return tierPoints; }
    public void setTierPoints(Integer tierPoints) { this.tierPoints = tierPoints; }

    public Boolean getReceivePromotionEmail() { return receivePromotionEmail; }
    public void setReceivePromotionEmail(Boolean receivePromotionEmail) { this.receivePromotionEmail = receivePromotionEmail; }
}
