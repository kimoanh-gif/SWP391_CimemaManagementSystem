package com.cinema.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Form DTO for creating and updating a Showtime (Screen 59 / UC-30).
 */
public class ShowtimeForm {

    private Integer showtimeId;

    @NotNull(message = "Vui lòng chọn phim")
    @Positive(message = "Mã phim phải lớn hơn 0")
    private Integer movieId;

    @NotNull(message = "Vui lòng chọn phòng chiếu")
    @Positive(message = "Mã phòng chiếu phải lớn hơn 0")
    private Integer roomId;

    @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    @NotNull(message = "Vui lòng nhập giá vé cơ sở")
    @DecimalMin(value = "0.0", message = "Giá vé cơ sở không được âm")
    @DecimalMax(value = "10000000.0", message = "Giá vé cơ sở không được vượt quá 10,000,000 VNĐ")
    private BigDecimal baseTicketPrice;

    @Pattern(
            regexp = "^(Scheduled|Showing|Ended|Cancelled|Stopped)$",
            message = "Trạng thái suất chiếu không hợp lệ"
    )
    private String status = "Scheduled";

    public ShowtimeForm() {
    }

    public Integer getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Integer showtimeId) {
        this.showtimeId = showtimeId;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getBaseTicketPrice() {
        return baseTicketPrice;
    }

    public void setBaseTicketPrice(BigDecimal baseTicketPrice) {
        this.baseTicketPrice = baseTicketPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
