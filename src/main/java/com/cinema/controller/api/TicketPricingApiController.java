package com.cinema.controller.api;

import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.SeatTypeNotFoundException;
import com.cinema.exception.TicketPricingNotFoundException;
import com.cinema.dto.ApiResponse;
import com.cinema.model.SeatType;
import com.cinema.dto.SeatTypeForm;
import com.cinema.model.TicketPricing;
import com.cinema.dto.TicketPricingForm;
import com.cinema.service.TicketPricingService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Screen 60: Ticket Pricing Management (UC-31).
 */
@RestController
@RequestMapping("/api/manager/pricing")
public class TicketPricingApiController {

    private final TicketPricingService pricingService;

    public TicketPricingApiController(TicketPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPricingData(
            @RequestParam(required = false) String dayType,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<TicketPricing> rules = pricingService.getAllPricingRules(dayType, status);
        List<SeatType> seatTypes = pricingService.getAllSeatTypes("Active");
        Map<Integer, Map<Integer, BigDecimal>> matrix = pricingService.generatePricingMatrix();
        BigDecimal atp = pricingService.getAverageTicketPrice();

        Map<String, Object> data = new HashMap<>();
        data.put("pricingRules", rules);
        data.put("seatTypes", seatTypes);
        data.put("matrix", matrix);
        data.put("averageTicketPrice", atp);

        return ResponseEntity.ok(ApiResponse.ok("Lấy dữ liệu bảng giá thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketPricing>> getPricingById(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        TicketPricing pricing = pricingService.getPricingById(id);
        return ResponseEntity.ok(ApiResponse.ok("Chi tiết cấu hình giá vé", pricing));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketPricing>> createPricing(
            @Valid @RequestBody TicketPricingForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        TicketPricing created = pricingService.createPricing(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo cấu hình giá vé thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketPricing>> updatePricing(
            @PathVariable int id,
            @Valid @RequestBody TicketPricingForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        TicketPricing updated = pricingService.updatePricing(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật cấu hình giá vé thành công", updated));
    }

    @PutMapping("/{id}/base-price")
    public ResponseEntity<ApiResponse<TicketPricing>> updateBasePrice(
            @PathVariable int id,
            @RequestParam BigDecimal basePrice,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        TicketPricing updated = pricingService.updateBasePrice(id, basePrice);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật giá cơ sở thành công", updated));
    }

    @PutMapping("/seat-type/{id}/multiplier")
    public ResponseEntity<ApiResponse<SeatType>> updateSeatTypeMultiplier(
            @PathVariable int id,
            @RequestParam BigDecimal multiplier,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType updated = pricingService.updateSeatTypeMultiplier(id, multiplier);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hệ số giá ghế thành công", updated));
    }

    @PostMapping("/seat-type")
    public ResponseEntity<ApiResponse<SeatType>> createSeatType(
            @Valid @RequestBody SeatTypeForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType created = pricingService.createSeatType(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo loại ghế mới thành công", created));
    }

    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculatePrice(
            @RequestParam BigDecimal basePrice,
            @RequestParam BigDecimal multiplier,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        BigDecimal calculated = pricingService.calculateTicketUnitPrice(basePrice, multiplier);
        Map<String, Object> res = new HashMap<>();
        res.put("basePrice", basePrice);
        res.put("multiplier", multiplier);
        res.put("calculatedPrice", calculated);
        return ResponseEntity.ok(ApiResponse.ok("Tính giá vé thành công (BR-50)", res));
    }

    @ExceptionHandler(TicketPricingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(TicketPricingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(SeatTypeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSeatTypeNotFound(SeatTypeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPricingConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(InvalidPricingConfigurationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.cinema.exception.DuplicatePricingRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicatePricingRule(com.cinema.exception.DuplicatePricingRuleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.cinema.exception.UnauthorizedManagerException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(com.cinema.exception.UnauthorizedManagerException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }
}
