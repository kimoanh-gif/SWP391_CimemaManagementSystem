package com.cinema.controller.api;

import com.cinema.exception.InvalidMovieStatusException;
import com.cinema.exception.MovieHasActiveShowtimesException;
import com.cinema.exception.MovieNotFoundException;
import com.cinema.exception.UnauthorizedManagerException;
import com.cinema.model.ApiResponse;
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
        return ResponseEntity.ok(ApiResponse.ok("Movie list retrieved successfully", movies));
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
        return ResponseEntity.ok(ApiResponse.ok("Available movies for showtimes retrieved successfully", movies));
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
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid input data", errors));
        }

        Movie created = movieService.createMovie(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Movie created successfully", created));
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
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid input data", errors));
        }

        Movie updated = movieService.updateMovie(id, form);
        return ResponseEntity.ok(ApiResponse.ok("Movie updated successfully", updated));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Movie>> deactivateMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Movie deactivated = movieService.deactivateMovie(id);
        return ResponseEntity.ok(ApiResponse.ok("Movie deactivated successfully", deactivated));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Movie>> reactivateMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        Movie reactivated = movieService.reactivateMovie(id);
        return ResponseEntity.ok(ApiResponse.ok("Movie '" + reactivated.getTitle() + "' reactivated successfully", reactivated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Movie>> updateStatus(
            @PathVariable int id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        String status = body.get("status");
        if (status == null || !status.trim().matches("^(Upcoming|Now Showing|Ended|Archived)$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Status cannot be empty"));
        }

        Movie updated = movieService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", updated));
    }

    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<List<Movie>>> getCustomerMovies() {
        List<Movie> movies = movieService.getCustomerMovies();
        return ResponseEntity.ok(ApiResponse.ok("Customer movies retrieved successfully", movies));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(
            @PathVariable int id,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        movieService.deleteMovie(id);
        return ResponseEntity.ok(ApiResponse.ok("Movie archived / deactivated successfully", null));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadMovieImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "type", defaultValue = "poster") String type,
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
        Map<String, String> allowedExtensions = Map.of(
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
            java.nio.file.Path directory = java.nio.file.Paths.get("uploads", "movies").toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(directory);

            String safeType = "banner".equalsIgnoreCase(type) ? "banner" : "poster";
            String fileName = "movie-" + safeType + "-" + java.util.UUID.randomUUID() + extension;
            java.nio.file.Path targetPath = directory.resolve(fileName).normalize();

            if (!targetPath.startsWith(directory)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid file save path"));
            }

            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/movies/" + fileName;

            return ResponseEntity.ok(ApiResponse.ok("Image uploaded successfully", Map.of("url", url)));
        } catch (java.io.IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Could not save image file: " + ex.getMessage()));
        }
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
