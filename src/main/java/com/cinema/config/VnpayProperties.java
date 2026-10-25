package com.cinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment.vnpay")
public class VnpayProperties {
    private boolean enabled;
    private String version = "2.1.0";
    private String payUrl;
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private String ipnUrl;
    private String bankCode;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }
    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getIpnUrl() { return ipnUrl; }
    public void setIpnUrl(String ipnUrl) { this.ipnUrl = ipnUrl; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public boolean isReady() {
        return enabled && hasText(payUrl) && hasText(tmnCode) && hasText(hashSecret) && hasText(returnUrl);
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
