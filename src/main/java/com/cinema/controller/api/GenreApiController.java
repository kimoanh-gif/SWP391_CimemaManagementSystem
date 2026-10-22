package com.cinema.controller.api;

import com.cinema.exception.DuplicateGenreException;
import com.cinema.exception.GenreAssociationException;
import com.cinema.exception.GenreNotFoundException;
import com.cinema.exception.UnauthorizedManagerException;
import com.cinema.dto.ApiResponse;
import com.cinema.model.Genre;
import com.cinema.dto.GenreForm;
import com.cinema.model.MovieSummary;
import com.cinema.service.GenreService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for Genre Management (Screen 55).
 * Connects frontend dynamic components, modal dialogs, and AJAX workflows.
 */
@RestController
@RequestMapping("/api/manager/genres")
public class GenreApiController {

    private final GenreService genreService;

    public GenreApiController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Genre>>> getGenres(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Genre> genres = genreService.getAllGenres(keyword, status);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách thể loại", genres));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Genre>> getGenre(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Genre genre = genreService.getGenreById(id);
        return ResponseEntity.ok(ApiResponse.ok(genre));
    }

    @GetMapping("/{id}/movies")
    public ResponseEntity<ApiResponse<List<MovieSummary>>> getAssociatedMovies(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<MovieSummary> movies = genreService.getAssociatedMovies(id);
        return ResponseEntity.ok(ApiResponse.ok(movies));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Genre>> createGenre(
            @Valid @RequestBody GenreForm form,
            BindingResult bindingResult,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu nhập không hợp lệ", errors));
        }

        Genre created = genreService.createGenre(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm mới thể loại thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Genre>> updateGenre(
            @PathVariable int id,
            @Valid @RequestBody GenreForm form,
            BindingResult bindingResult,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu nhập không hợp lệ", errors));
        }

        Genre updated = genreService.updateGenre(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thể loại thành công", updated));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Genre>> toggleStatus(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Genre toggled = genreService.toggleStatus(id);
        String action = toggled.isActive() ? "kích hoạt" : "hủy kích hoạt";
        return ResponseEntity.ok(ApiResponse.ok("Đã " + action + " thể loại thành công", toggled));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Genre>> updateStatus(
            @PathVariable int id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        String status = body.get("status");
        if (status == null || (!status.equalsIgnoreCase("ACTIVE") && !status.equalsIgnoreCase("INACTIVE"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái phải là ACTIVE hoặc INACTIVE"));
        }

        Genre existing = genreService.getGenreById(id);
        GenreForm form = GenreForm.from(existing);
        form.setStatus(status.toUpperCase());
        Genre updated = genreService.updateGenre(id, form);

        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGenre(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        genreService.deleteGenre(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa thể loại thành công", null));
    }

    @ExceptionHandler(GenreNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(GenreNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateGenreException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateGenreException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(GenreAssociationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssociation(GenreAssociationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedManagerException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedManagerException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }
}
