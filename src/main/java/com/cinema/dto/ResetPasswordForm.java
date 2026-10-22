package com.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordForm {

    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Size(min = 8, max = 128, message = "Mật khẩu phải có độ dài 8-128 ký tự.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    public ResetPasswordForm() {
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
