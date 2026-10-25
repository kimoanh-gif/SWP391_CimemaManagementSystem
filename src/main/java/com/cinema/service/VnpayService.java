package com.cinema.service;

import com.cinema.config.VnpayProperties;
import com.cinema.dao.CustomerBookingDAO;
import com.cinema.model.PaymentView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class VnpayService {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;
    private final CustomerBookingDAO bookingDAO;

    public VnpayService(VnpayProperties properties, CustomerBookingDAO bookingDAO) {
        this.properties = properties;
        this.bookingDAO = bookingDAO;
    }

    public boolean isReady() { return properties.isReady(); }

    public String createPaymentUrl(PaymentView payment, HttpServletRequest request) {
        if (!properties.isReady()) throw new IllegalStateException("VNPay Sandbox is not fully configured.");
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime expiresAt = payment.expiresAt() == null ? now.plusMinutes(15) : payment.expiresAt();
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", properties.getVersion());
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", properties.getTmnCode());
        fields.put("vnp_Amount", payment.amount().movePointRight(2).setScale(0).toPlainString());
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_TxnRef", payment.transactionRef());
        fields.put("vnp_OrderInfo", "Thanh toan ve " + payment.bookingCode());
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_Locale", "vn");
        if (properties.getBankCode() != null && !properties.getBankCode().isBlank()) {
            fields.put("vnp_BankCode", properties.getBankCode().trim().toUpperCase());
        }
        fields.put("vnp_ReturnUrl", properties.getReturnUrl());
        fields.put("vnp_IpAddr", clientIp(request));
        fields.put("vnp_CreateDate", now.format(VNPAY_TIME));
        fields.put("vnp_ExpireDate", expiresAt.format(VNPAY_TIME));
        String query = canonical(fields);
        return properties.getPayUrl() + "?" + query + "&vnp_SecureHash=" + hmacSha512(query);
    }

    public boolean hasValidSignature(Map<String, String> parameters) {
        String supplied = parameters.get("vnp_SecureHash");
        if (supplied == null || supplied.isBlank()) return false;
        Map<String, String> signed = new TreeMap<>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType") && value != null && !value.isEmpty()) {
                signed.put(key, value);
            }
        });
        return MessageDigest.isEqual(hmacSha512(canonical(signed)).getBytes(StandardCharsets.US_ASCII),
                supplied.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    @Transactional
    public String processIpn(Map<String, String> parameters) {
        if (!hasValidSignature(parameters)) return "97";
        String transactionRef = parameters.get("vnp_TxnRef");
        Optional<PaymentView> found = bookingDAO.findPaymentByTransactionRef(transactionRef);
        if (found.isEmpty()) return "01";
        PaymentView payment = found.get();
        BigDecimal callbackAmount;
        try {
            callbackAmount = new BigDecimal(parameters.get("vnp_Amount")).movePointLeft(2);
        } catch (RuntimeException exception) {
            return "04";
        }
        if (payment.amount().compareTo(callbackAmount) != 0) return "04";
        if ("PAID".equalsIgnoreCase(payment.status())) return "02";
        if (!"PENDING".equalsIgnoreCase(payment.status())) return "02";
        boolean successful = "00".equals(parameters.get("vnp_ResponseCode"))
                && "00".equals(parameters.get("vnp_TransactionStatus"));
        bookingDAO.applyVnpayResult(payment.paymentId(), successful,
                parameters.get("vnp_TransactionNo"), parameters.get("vnp_ResponseCode"),
                parameters.get("vnp_TransactionStatus"), parameters.get("vnp_BankCode"),
                parameters.get("vnp_CardType"));
        return "00";
    }

    public Optional<PaymentView> findPayment(String transactionRef) {
        return bookingDAO.findPaymentByTransactionRef(transactionRef);
    }

    private String canonical(Map<String, String> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right).orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign the VNPay request.", exception);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String value = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return value == null || value.isBlank() || "0:0:0:0:0:0:0:1".equals(value) ? "127.0.0.1" : value;
    }
}
