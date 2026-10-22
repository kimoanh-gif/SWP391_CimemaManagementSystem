package com.cinema.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginForm {
    @NotBlank(message = "Vui lòng nhập email hoặc tên đăng nhập.")
    @Size(max = 255)
    private String identifier;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 128)
    private String password;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
