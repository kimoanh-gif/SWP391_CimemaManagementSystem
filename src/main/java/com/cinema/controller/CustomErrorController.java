package com.cinema.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.boot.web.servlet.error.ErrorController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
public class CustomErrorController implements ErrorController {

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, Model model) {
        request.setAttribute("exception", ex.getClass().getName() + ": " + ex.getMessage());
        request.setAttribute("trace", getStackTrace(ex));
        return "error";
    }

    @org.springframework.web.bind.annotation.RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status   = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errMsg   = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object excName  = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object trace    = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = 500;
        try { statusCode = Integer.parseInt(status != null ? String.valueOf(status) : "500"); } catch (Exception ignored) {}

        String message = errMsg != null ? String.valueOf(errMsg) : "Unknown error";
        if (message.isBlank()) {
            message = HttpStatusCode.valueOf(statusCode).toString();
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        model.addAttribute("status",    statusCode);
        model.addAttribute("error",    HttpStatus.valueOf(statusCode).getReasonPhrase());
        model.addAttribute("message",   message);
        model.addAttribute("path",     request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        model.addAttribute("timestamp", timestamp);

        if (trace instanceof Throwable t) {
            model.addAttribute("exception", t.getClass().getName() + ": " + t.getMessage());
            model.addAttribute("trace", getStackTrace(t));
        }

        return "error";
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
