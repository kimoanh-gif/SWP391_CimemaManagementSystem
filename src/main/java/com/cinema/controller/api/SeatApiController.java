package com.cinema.controller.api;

import com.cinema.model.ApiResponse;
import com.cinema.model.Seat;
import com.cinema.service.SeatService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for Screen 58: Seat Layout Management (UC-29).
 */
@RestController
@RequestMapping("/api/manager/seats")
public class SeatApiController {

    private final SeatService seatService;

    public SeatApiController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeatsByRoom(
            @PathVariable int roomId,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Seat> seats = seatService.getSeatsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy sơ đồ ghế thành công", seats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Seat>> getSeatById(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Seat seat = seatService.getSeatById(id);
        return ResponseEntity.ok(ApiResponse.ok("Chi tiết ghế", seat));
    }

    @PutMapping("/room/{roomId}/layout")
    public ResponseEntity<ApiResponse<List<Seat>>> saveLayout(
            @PathVariable int roomId,
            @RequestBody List<Seat> seats,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedUpdatedAt,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Seat> saved = seatService.saveRoomLayout(roomId, seats, expectedUpdatedAt);
        return ResponseEntity.ok(ApiResponse.ok("Lưu sơ đồ ghế thành công", saved));
    }


    @PostMapping("/room/{roomId}/generate-default")
    public ResponseEntity<ApiResponse<List<Seat>>> generateDefault(
            @PathVariable int roomId,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Seat> generated = seatService.generateDefaultLayout(roomId);
        return ResponseEntity.ok(ApiResponse.ok("Khởi tạo sơ đồ tiêu chuẩn thành công", generated));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Seat>> updateSeat(
            @PathVariable int id,
            @RequestParam(required = false) String seatTypeName,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Seat updated = seatService.updateSeat(id, seatTypeName, status);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật ghế thành công", updated));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(com.cinema.exception.InvalidSeatLayoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidSeatLayout(com.cinema.exception.InvalidSeatLayoutException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(com.cinema.exception.RoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomNotFound(com.cinema.exception.RoomNotFoundException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }
}

