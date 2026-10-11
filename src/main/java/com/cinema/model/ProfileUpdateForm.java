package com.cinema.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ProfileUpdateForm {

    @NotBlank(message = "Full name is required.")
    @Size(max = 100, message = "Full name must not exceed 100 characters.")
    private String fullName;

    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9,10}$",
            message = "Enter a valid Vietnamese phone number."
    )
    private String phone;

    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Address must not exceed 255 characters.")
    private String address;

    public static ProfileUpdateForm from(CustomerProfile profile) {
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName(profile.getFullName());
        form.setPhone(profile.getPhone());
        form.setDateOfBirth(profile.getDateOfBirth());
        form.setAddress(profile.getAddress());
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
}