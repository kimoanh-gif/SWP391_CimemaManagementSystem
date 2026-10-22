package com.cinema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotForm {

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    public ForgotForm() {
    }

    public ForgotForm(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
