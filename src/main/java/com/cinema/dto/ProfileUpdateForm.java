package com.cinema.dto;

import com.cinema.model.CustomerProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ProfileUpdateForm {

    @NotBlank(message = "Username is required.")
    @Pattern(regexp = "^[A-Za-z0-9._-]{4,50}$", message = "Use 4–50 letters, numbers, dots, underscores, or hyphens.")
    private String username;

    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
    private String fullName;

    @Pattern(regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$", message = "Enter a valid Vietnamese phone number.")
    private String phone;

    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    public static ProfileUpdateForm from(CustomerProfile profile) {
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setUsername(profile.getUsername());
        form.setFullName(profile.getFullName());
        form.setPhone(profile.getPhone());
        form.setDateOfBirth(profile.getDateOfBirth());
        return form;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim(); }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone == null || phone.isBlank() ? null : phone.trim(); }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

}
