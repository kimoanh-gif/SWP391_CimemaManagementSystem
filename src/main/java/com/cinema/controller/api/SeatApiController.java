package com.cinema.controller.api;

import com.cinema.dto.SeatLayoutForm;
import com.cinema.dto.SeatUpdateForm;
import com.cinema.model.ApiResponse;
import com.cinema.model.Seat;
import com.cinema.service.SeatService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for Screen 58: Seat Layout Management (UC-29).
 */
@RestController
@RequestMapping("/api/manager/seats")
@Validated
public class SeatApiController {

    private final SeatService seatService;

    public SeatApiController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeatsByRoom(
            @PathVariable @Positive(message = "Room ID must be greater than 0.") int roomId,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Seat> seats = seatService.getSeatsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.ok("Seat map retrieved successfully", seats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Seat>> getSeatById(
            @PathVariable @Positive(message = "Seat ID must be greater than 0.") int id,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Seat seat = seatService.getSeatById(id);
        return ResponseEntity.ok(ApiResponse.ok("Seat details", seat));
    }

    @PutMapping("/room/{roomId}/layout")
    public ResponseEntity<ApiResponse<List<Seat>>> saveLayout(
            @PathVariable @Positive(message = "Room ID must be greater than 0.") int roomId,
            @RequestBody @NotEmpty(message = "Seat layout cannot be empty.") List<@Valid SeatLayoutForm> seatForms,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedUpdatedAt,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Seat> seats = seatForms.stream()
                .map(form -> form.toSeat(roomId))
                .collect(Collectors.toList());
        List<Seat> saved = seatService.saveRoomLayout(roomId, seats, expectedUpdatedAt);
        return ResponseEntity.ok(ApiResponse.ok("Seat layout saved successfully", saved));
    }

    @PostMapping("/room/{roomId}/generate-default")
    public ResponseEntity<ApiResponse<List<Seat>>> generateDefault(
            @PathVariable @Positive(message = "Room ID must be greater than 0.") int roomId,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Seat> generated = seatService.generateDefaultLayout(roomId);
        return ResponseEntity.ok(ApiResponse.ok("Standard seat layout initialized successfully", generated));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Seat>> updateSeat(
            @PathVariable @Positive(message = "Seat ID must be greater than 0.") int id,
            @Valid @ModelAttribute SeatUpdateForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            String message = errors.isEmpty() ? "Invalid seat update data" : errors.get(0);
            return ResponseEntity.badRequest().body(ApiResponse.error(message, errors));
        }
        Seat updated = seatService.updateSeat(id, form.getSeatTypeName(), form.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Seat updated successfully", updated));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        String msg = errors.isEmpty() ? "Invalid input data." : errors.get(0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        String msg = errors.isEmpty() ? "Invalid input data." : errors.get(0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg, errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> errors = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
        String msg = errors.isEmpty() ? "Invalid input data." : errors.get(0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg, errors));
    }

    @ExceptionHandler(com.cinema.exception.InvalidSeatLayoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidSeatLayout(
            com.cinema.exception.InvalidSeatLayoutException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.cinema.exception.RoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomNotFound(com.cinema.exception.RoomNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
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
