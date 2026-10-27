package com.cinema.controller.api;

import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.ApiResponse;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import com.cinema.service.RoomService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Room management – used by AJAX calls from the Manager UI.
 */
@RestController
@RequestMapping("/api/manager/rooms")
public class RoomApiController {

    private final RoomService roomService;

    public RoomApiController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Room>>> list(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String status,
                                                         jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Room> rooms = roomService.getAll(keyword, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Thành công", rooms, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> get(@PathVariable int id,
                                                  jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Room room = roomService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Thành công", room, null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody RoomForm form,
                                                    jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        roomService.create(form);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tạo phòng thành công", null, null));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable int id,
                                                    @Valid @RequestBody RoomForm form,
                                                    jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        form.setRoomId(id);
        roomService.update(form);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật phòng thành công", null, null));
    }

    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable int id,
                                                        jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        roomService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Phòng đã được đưa vào bảo trì", null, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int id,
                                                    jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        roomService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xóa phòng chiếu thành công", null, null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(new ApiResponse<>(false, ex.getMessage(), null, null));
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RoomNotFoundException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, ex.getMessage(), null, null));
    }

    @ExceptionHandler(com.cinema.exception.RoomHasActiveShowtimesException.class)
    public ResponseEntity<ApiResponse<Void>> handleActiveShowtimes(com.cinema.exception.RoomHasActiveShowtimesException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(new ApiResponse<>(false, ex.getMessage(), null, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, ex.getMessage(), null, null));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = "Dữ liệu phòng không hợp lệ hoặc bị trùng lặp.";
        Throwable cause = ex.getMostSpecificCause();
        String specificMsg = cause != null && cause.getMessage() != null ? cause.getMessage() : "";
        if (specificMsg.contains("UQ_Room_RoomCode")) {
            msg = "Mã phòng chiếu đã tồn tại trong hệ thống. Vui lòng nhập mã phòng khác.";
        } else if (specificMsg.contains("UQ_Room_RoomName")) {
            msg = "Tên phòng chiếu đã tồn tại trong hệ thống. Vui lòng đặt tên phòng khác.";
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(new ApiResponse<>(false, msg, null, null));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dữ liệu nhập không hợp lệ.");
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, errorMsg, null, null));
    }

    @ExceptionHandler(com.cinema.exception.UnauthorizedManagerException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(com.cinema.exception.UnauthorizedManagerException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(new ApiResponse<>(false, ex.getMessage(), null, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Đã xảy ra lỗi: " + ex.getMessage(), null, null));
    }
}
