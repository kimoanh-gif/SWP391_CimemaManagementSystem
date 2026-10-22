package com.cinema.controller.api;

import com.cinema.exception.InvalidMovieStatusException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.exception.UnauthorizedManagerException;
import com.cinema.dto.ApiResponse;
import com.cinema.model.Movie;
import com.cinema.dto.MovieForm;
import com.cinema.service.MovieService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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
 * REST API for Screen 54: Movie Management.
 */
@RestController
@RequestMapping("/api/manager/movies")
public class MovieApiController {

    private final MovieService movieService;

    public MovieApiController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false, defaultValue = "ALL") String ageRating,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Movie> movies = movieService.getAllMovies(keyword, genreId, ageRating, status);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách phim", movies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> getMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Movie movie = movieService.getMovieById(id);
        return ResponseEntity.ok(ApiResponse.ok(movie));
    }

    @GetMapping("/available-for-showtimes")
    public ResponseEntity<ApiResponse<List<Movie>>> getAvailableMoviesForShowtimes(
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Movie> movies = movieService.getAvailableMoviesForShowtimes();
        return ResponseEntity.ok(ApiResponse.ok("Danh sách phim có thể tạo suất chiếu", movies));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Movie>> createMovie(
            @Valid @RequestBody MovieForm form,
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

        Movie created = movieService.createMovie(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm mới phim thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> updateMovie(
            @PathVariable int id,
            @Valid @RequestBody MovieForm form,
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

        Movie updated = movieService.updateMovie(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin phim thành công", updated));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Movie>> deactivateMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Movie deactivated = movieService.deactivateMovie(id);
        return ResponseEntity.ok(ApiResponse.ok("Hủy kích hoạt phim thành công", deactivated));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Movie>> reactivateMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Movie reactivated = movieService.reactivateMovie(id);
        return ResponseEntity.ok(ApiResponse.ok("Mở lại và kích hoạt phim '" + reactivated.getTitle() + "' thành công", reactivated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Movie>> updateStatus(
            @PathVariable int id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        String status = body.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái không được để trống"));
        }

        Movie updated = movieService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", updated));
    }

    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<List<Movie>>> getCustomerMovies() {
        List<Movie> movies = movieService.getCustomerMovies();
        return ResponseEntity.ok(ApiResponse.ok("Danh sách phim đang chiếu và sắp chiếu dành cho khách hàng", movies));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        movieService.deleteMovie(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu trữ / hủy kích hoạt phim thành công", null));
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(MovieNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MovieHasActiveShowtimesException.class)
    public ResponseEntity<ApiResponse<Void>> handleActiveShowtimes(MovieHasActiveShowtimesException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidMovieStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatus(InvalidMovieStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedManagerException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedManagerException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }
}
