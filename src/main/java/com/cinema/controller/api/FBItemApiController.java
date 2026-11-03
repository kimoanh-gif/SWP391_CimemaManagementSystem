package com.cinema.controller.api;

import com.cinema.exception.FBItemNotFoundException;
import com.cinema.exception.InvalidFBItemException;
import com.cinema.model.ApiResponse;
import com.cinema.model.FBItem;
import com.cinema.dto.FBItemForm;
import com.cinema.service.FBItemService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        return ResponseEntity.ok(ApiResponse.ok("F&B product list retrieved successfully", items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FBItem>> getItemById(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem item = fbItemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.ok("F&B product details", item));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FBItem>> createItem(
            @Valid @RequestBody FBItemForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem created = fbItemService.createItem(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("F&B product created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FBItem>> updateItem(
            @PathVariable int id,
            @Valid @RequestBody FBItemForm form,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.updateItem(id, form);
        return ResponseEntity.ok(ApiResponse.ok("F&B product updated successfully", updated));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<FBItem>> updatePrice(
            @PathVariable int id,
            @RequestParam BigDecimal sellingPrice,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.updatePrice(id, sellingPrice);
        return ResponseEntity.ok(ApiResponse.ok("Selling price updated successfully", updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FBItem>> updateStatus(
            @PathVariable int id,
            @RequestParam String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", updated));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<FBItem>> deactivateItem(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        FBItem updated = fbItemService.deactivateItem(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deactivated successfully", updated));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> uploadFBImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please select an image file"));
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Maximum image size is 5 MB"));
        }

        String contentType = file.getContentType();
        java.util.Map<String, String> allowedExtensions = java.util.Map.of(
                "image/jpeg", ".jpg",
                "image/png", ".png",
                "image/webp", ".webp",
                "image/gif", ".gif"
        );

        String extension = contentType != null ? allowedExtensions.get(contentType.toLowerCase()) : null;
        if (extension == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Only JPG, PNG, WebP, GIF image formats are supported"));
        }

        try {
            java.nio.file.Path directory = java.nio.file.Paths.get("uploads", "fb").toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(directory);

            String fileName = "fb-" + java.util.UUID.randomUUID() + extension;
            java.nio.file.Path targetPath = directory.resolve(fileName).normalize();

            if (!targetPath.startsWith(directory)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid file save path"));
            }

            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/fb/" + fileName;

            return ResponseEntity.ok(ApiResponse.ok("Image uploaded successfully", java.util.Map.of("url", url)));
        } catch (java.io.IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Cannot save image file: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        fbItemService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.ok("F&B product deleted successfully", null));
    }

    @ExceptionHandler(FBItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(FBItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidFBItemException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(InvalidFBItemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(java.util.stream.Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(com.cinema.exception.UnauthorizedManagerException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(com.cinema.exception.UnauthorizedManagerException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }
}
