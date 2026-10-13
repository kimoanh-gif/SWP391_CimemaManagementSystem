package com.cinema.controller.api;

import com.cinema.model.ApiResponse;
import com.cinema.model.SeatType;
import com.cinema.model.SeatTypeForm;
import com.cinema.service.TicketPricingService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST API Controller for Screen 57: Seat Type Management (UC-28).
 */
@RestController
@RequestMapping("/api/manager/seat-types")
public class SeatTypeApiController {

    private final TicketPricingService pricingService;

    public SeatTypeApiController(TicketPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatType>>> getAllSeatTypes(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<SeatType> list = pricingService.getAllSeatTypes(status);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách loại ghế thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SeatType>> getSeatTypeById(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType st = pricingService.getSeatTypeById(id);
        return ResponseEntity.ok(ApiResponse.ok("Chi tiết loại ghế", st));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SeatType>> createSeatType(
            @Valid @RequestBody SeatTypeForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType created = pricingService.createSeatType(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo loại ghế mới thành công", created));
    }

    @PutMapping("/{id}/multiplier")
    public ResponseEntity<ApiResponse<SeatType>> updateMultiplier(
            @PathVariable int id,
            @RequestParam BigDecimal multiplier,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType updated = pricingService.updateSeatTypeMultiplier(id, multiplier);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hệ số giá ghế thành công", updated));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SeatType>> updateSeatType(
            @PathVariable int id,
            @Valid @RequestBody SeatTypeForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType updated = pricingService.updateSeatType(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin loại ghế thành công", updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SeatType>> updateStatus(
            @PathVariable int id,
            @RequestParam String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        SeatType updated = pricingService.updateSeatTypeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái loại ghế thành công", updated));
    }
}
