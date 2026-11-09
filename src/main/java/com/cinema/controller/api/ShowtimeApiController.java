package com.cinema.controller.api;

import com.cinema.exception.InvalidShowtimeException;
import com.cinema.exception.ShowtimeConflictException;
import com.cinema.exception.ShowtimeNotFoundException;
import com.cinema.model.ApiResponse;
import com.cinema.model.Showtime;
import com.cinema.dto.BatchShowtimeRequest;
import com.cinema.dto.ShowtimeForm;
import com.cinema.service.ShowtimeService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * REST API Controller for Screen 59: Showtime Management (UC-30).
 */
@RestController
@RequestMapping("/api/manager/showtimes")
public class ShowtimeApiController {

    private final ShowtimeService showtimeService;

    public ShowtimeApiController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Showtime>>> getShowtimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer movieId,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Showtime> list = showtimeService.getAllShowtimes(date, roomId, movieId, status);
        return ResponseEntity.ok(ApiResponse.ok("Showtime list retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Showtime>> getShowtimeById(
            @PathVariable int id,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Showtime st = showtimeService.getShowtimeById(id);
        return ResponseEntity.ok(ApiResponse.ok("Showtime details", st));
    }

    @GetMapping("/room-schedule")
    public ResponseEntity<ApiResponse<List<Showtime>>> getRoomSchedule(
            @RequestParam int roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Showtime> schedule = showtimeService.getRoomScheduleForDate(roomId, date);
        return ResponseEntity.ok(ApiResponse.ok("Room schedule loaded successfully", schedule));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<Showtime>>> createShowtimeBatch(
            @Valid @RequestBody BatchShowtimeRequest batchRequest,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Showtime> created = showtimeService.createShowtimeBatch(
                batchRequest.getRoomId(),
                batchRequest.getShowtimes()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Schedule saved successfully! " + created.size() + " showtimes added.", created));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Showtime>> createShowtime(
            @Valid @RequestBody ShowtimeForm form,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Showtime created = showtimeService.createShowtime(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Showtime created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Showtime>> updateShowtime(
            @PathVariable int id,
            @Valid @RequestBody ShowtimeForm form,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Showtime updated = showtimeService.updateShowtime(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Showtime updated successfully", updated));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<ApiResponse<Showtime>> stopShowtime(
            @PathVariable int id,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Showtime stopped = showtimeService.stopShowtime(id);
        return ResponseEntity.ok(ApiResponse.ok("Showtime stopped successfully (BR-54)", stopped));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Showtime>> cancelShowtime(
            @PathVariable int id,
            HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Showtime cancelled = showtimeService.cancelShowtime(id);
        return ResponseEntity.ok(ApiResponse.ok("Showtime cancelled successfully", cancelled));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .toList();
        String msg = errors.isEmpty() ? "Invalid showtime information" : errors.get(0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg, errors));
    }

    @ExceptionHandler(ShowtimeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ShowtimeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ShowtimeConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ShowtimeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidShowtimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(InvalidShowtimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
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
