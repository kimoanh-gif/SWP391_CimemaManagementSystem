package com.cinema.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpForm {
    @NotBlank(message = "Vui lòng nhập mã OTP.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP gồm 6 chữ số.")
    private String otp;

    public OtpForm() {}
    public OtpForm(String otp) { this.otp = otp; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
