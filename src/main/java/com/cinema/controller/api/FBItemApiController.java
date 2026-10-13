package com.cinema.controller.api;

import com.cinema.exception.FBItemNotFoundException;
import com.cinema.exception.InvalidFBItemException;
import com.cinema.model.ApiResponse;
import com.cinema.model.FBItem;
import com.cinema.model.FBItemForm;
import com.cinema.service.FBItemService;
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
import java.util.List;

/**
 * REST API Controller for Screen 61: Food & Beverage Management (UC-32).
 */
@RestController
@RequestMapping("/api/manager/fb")
public class FBItemApiController {

    private final FBItemService fbItemService;

    public FBItemApiController(FBItemService fbItemService) {
        this.fbItemService = fbItemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FBItem>>> getAllItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<FBItem> items = fbItemService.getAllItems(keyword, category, status);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách sản phẩm F&B thành công", items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FBItem>> getItemById(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem item = fbItemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.ok("Chi tiết sản phẩm F&B", item));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FBItem>> createItem(
            @Valid @RequestBody FBItemForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem created = fbItemService.createItem(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo sản phẩm F&B mới thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FBItem>> updateItem(
            @PathVariable int id,
            @Valid @RequestBody FBItemForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.updateItem(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật sản phẩm F&B thành công", updated));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<FBItem>> updatePrice(
            @PathVariable int id,
            @RequestParam BigDecimal sellingPrice,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.updatePrice(id, sellingPrice);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật giá bán thành công", updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FBItem>> updateStatus(
            @PathVariable int id,
            @RequestParam String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", updated));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<FBItem>> deactivateItem(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.deactivateItem(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ngừng kinh doanh sản phẩm thành công", updated));
    }

    @ExceptionHandler(FBItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(FBItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidFBItemException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(InvalidFBItemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }
}
