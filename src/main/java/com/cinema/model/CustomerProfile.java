package com.cinema.model;

import java.time.LocalDate;

public class CustomerProfile {

    private int accountId;
    private int customerId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String membershipRankName;
    private int availablePoints;
    private int tierPoints;

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMembershipRankName() {
        return membershipRankName;
    }

    public void setMembershipRankName(String membershipRankName) {
        this.membershipRankName = membershipRankName;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public void setAvailablePoints(int availablePoints) {
        this.availablePoints = availablePoints;
    }

    public int getTierPoints() {
        return tierPoints;
    }

    public void setTierPoints(int tierPoints) {
        this.tierPoints = tierPoints;
    }
}