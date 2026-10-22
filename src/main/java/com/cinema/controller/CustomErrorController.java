package com.cinema.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Controller
public class CustomErrorController implements ErrorController {

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
        return "error";
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
            case 400 -> new ErrorCopy("Yêu cầu chưa hợp lệ", "Thông tin gửi lên chưa đúng định dạng. Vui lòng kiểm tra và thử lại.");
            case 401 -> new ErrorCopy("Bạn cần đăng nhập", "Phiên đăng nhập có thể đã hết hạn. Hãy đăng nhập để tiếp tục.");
            case 403 -> new ErrorCopy("Không có quyền truy cập", "Tài khoản của bạn không được phép mở khu vực này.");
            case 404 -> new ErrorCopy("Không tìm thấy trang", "Suất chiếu này có vẻ đã kết thúc, hoặc địa chỉ bạn mở không tồn tại.");
            case 405 -> new ErrorCopy("Thao tác không được hỗ trợ", "Trang này không chấp nhận phương thức vừa được sử dụng.");
            case 429 -> new ErrorCopy("Bạn thao tác hơi nhanh", "Vui lòng nghỉ một chút rồi thử lại.");
            case 503 -> new ErrorCopy("Hệ thống đang bảo trì", "Dịch vụ tạm thời chưa sẵn sàng. Vui lòng quay lại sau ít phút.");
            default -> new ErrorCopy(reason, "Đã có sự cố ngoài dự kiến. Vui lòng quay lại trang chủ hoặc thử lại.");
        };
    }

    private record ErrorCopy(String headline, String message) {}
}
