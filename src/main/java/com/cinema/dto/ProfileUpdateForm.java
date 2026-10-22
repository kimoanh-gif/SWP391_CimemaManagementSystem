package com.cinema.dto;

import com.cinema.model.CustomerProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ProfileUpdateForm {

    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
    private String fullName;

    @Pattern(
            regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$",
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
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null || phone.isBlank() ? null : phone.trim();
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
        this.address = address == null || address.isBlank() ? null : address.trim();
    }
}
