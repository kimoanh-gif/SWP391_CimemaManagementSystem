package com.cinema.controller;

import com.cinema.service.VnpayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
public class VnpayIpnController {
    private final VnpayService vnpayService;

    public VnpayIpnController(VnpayService vnpayService) {
        this.vnpayService = vnpayService;
    }

    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> receiveIpn(@RequestParam Map<String, String> parameters) {
        String code;
        try {
            code = vnpayService.processIpn(parameters);
        } catch (RuntimeException exception) {
            code = "99";
        }
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", code);
        response.put("Message", message(code));
        return ResponseEntity.ok(response);
    }

    private String message(String code) {
        return switch (code) {
            case "00" -> "Confirm Success";
            case "01" -> "Order not Found";
            case "02" -> "Order already confirmed";
            case "04" -> "Invalid Amount";
            case "97" -> "Invalid Checksum";
            default -> "Unknown error";
        };
    }
}
