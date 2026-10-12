package com.cinema.controller.api;

import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.ApiResponse;
import com.cinema.model.Room;
import com.cinema.model.RoomForm;
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

    @PutMapping("/{id}")
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

    // Global exception handler for RoomNotFoundException
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RoomNotFoundException ex) {
        return ResponseEntity.status(404).body(new ApiResponse<>(false, ex.getMessage(), null, null));
    }
}
