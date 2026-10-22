package com.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpForm {

    @NotBlank(message = "Mã OTP không được để trống.")
    @Size(min = 6, max = 6, message = "Mã OTP gồm 6 chữ số.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP chỉ chứa 6 chữ số.")
    private String otp;

    public OtpForm() {
    }

    public OtpForm(String otp) {
        this.otp = otp;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
