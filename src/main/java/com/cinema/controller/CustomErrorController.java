package com.cinema.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Controller
public class CustomErrorController implements ErrorController {
    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    private static final DateTimeFormatter UTC_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        int statusCode = resolveStatusCode(request);
        HttpStatus status = HttpStatus.resolve(statusCode);
        String reason = status != null ? status.getReasonPhrase() : "Unexpected Error";

        model.addAttribute("status", statusCode);
        model.addAttribute("error", reason);
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        model.addAttribute("timestamp", UTC_TIME.format(Instant.now()));

        if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            model.addAttribute("rayId", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            model.addAttribute("host", request.getServerName());
            return "error-500";
        }

        ErrorCopy copy = errorCopy(statusCode, reason);
        model.addAttribute("headline", copy.headline());
        model.addAttribute("message", copy.message());
        addErrorDiagnostics(request, statusCode, reason, model);
        return "error";
    }

    private void addErrorDiagnostics(HttpServletRequest request, int statusCode, String reason, Model model) {
        Object exceptionValue = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Throwable exception = exceptionValue instanceof Throwable throwable ? throwable : null;
        String dispatcherMessage = cleanDiagnostic(request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
        String errorType = exception != null ? exception.getClass().getSimpleName() : "HTTP " + statusCode;
        String errorDetail = exception != null ? cleanDiagnostic(exception.getMessage()) : dispatcherMessage;
        if (errorDetail == null || errorDetail.isBlank()) errorDetail = reason;

        String rootCause = null;
        if (exception != null) {
            Throwable root = exception;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            if (root != exception) rootCause = cleanDiagnostic(root.getMessage());
            log.warn("Request failed with status {} at {}: {}", statusCode,
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), errorDetail, exception);
        } else {
            log.warn("Request failed with status {} at {}: {}", statusCode,
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), errorDetail);
        }
        model.addAttribute("errorType", errorType);
        model.addAttribute("errorDetail", errorDetail);
        model.addAttribute("rootCause", rootCause);
    }

    private String cleanDiagnostic(Object value) {
        if (value == null) return null;
        String text = value.toString().replaceAll("[\\r\\n\\t]+", " ").trim();
        text = text.replaceAll("(?i)(password|passwd|pwd|secret|token|authorization)(\\s*[=:]\\s*)[^ ,;]+", "$1$2[hidden]");
        // Keep the actual failing message readable without exposing a complete trace or huge SQL payload.
        return text.length() > 600 ? text.substring(0, 600) + "…" : text;
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? 500 : Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 500;
        }
    }

    private ErrorCopy errorCopy(int statusCode, String reason) {
        return switch (statusCode) {
            case 400 -> new ErrorCopy("Invalid request", "The submitted information is not valid. Check it and try again.");
            case 401 -> new ErrorCopy("Sign-in required", "Your session may have expired. Sign in to continue.");
            case 403 -> new ErrorCopy("Access denied", "Your account does not have permission to open this area.");
            case 404 -> new ErrorCopy("Page not found", "The requested page may have moved, expired, or does not exist.");
            case 405 -> new ErrorCopy("Action not supported", "This page does not accept the request method that was used.");
            case 429 -> new ErrorCopy("Too many requests", "Wait a moment before trying again.");
            case 503 -> new ErrorCopy("Service temporarily unavailable", "The service is not ready right now. Try again in a few minutes.");
            default -> new ErrorCopy(reason, "An unexpected error occurred. Return home or try again.");
        };
    }

    private record ErrorCopy(String headline, String message) {}
}
