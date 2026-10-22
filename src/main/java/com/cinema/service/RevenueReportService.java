package com.cinema.service;

import com.cinema.dao.RevenueReportDao;
import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.MovieRevenueItem;
import com.cinema.model.report.RevenueReportData;
import com.cinema.dto.RevenueReportRequest;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.model.report.TransactionRevenueItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service for generating Revenue Reports and exporting report data (Screen 47 & 49).
 */
@Service
public class RevenueReportService {

    private final RevenueReportDao revenueReportDao;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public RevenueReportService(RevenueReportDao revenueReportDao) {
        this.revenueReportDao = revenueReportDao;
    }

    /**
     * Resolves and validates date ranges based on preset or custom dates.
     */
    public void resolveAndValidateDates(RevenueReportRequest request) {
        if (request == null) return;

        LocalDate today = LocalDate.now();
        String preset = request.getPreset();

        if (preset != null && !preset.isBlank() && !"custom".equalsIgnoreCase(preset)) {
            switch (preset.toLowerCase().trim()) {
                case "today":
                    request.setStartDate(today);
                    request.setEndDate(today);
                    break;
                case "last7days":
                    request.setStartDate(today.minusDays(6));
                    request.setEndDate(today);
                    break;
                case "thismonth":
                    request.setStartDate(today.withDayOfMonth(1));
                    request.setEndDate(today.withDayOfMonth(today.lengthOfMonth()));
                    break;
                case "thisquarter":
                    int currentMonth = today.getMonthValue();
                    int firstMonthOfQuarter = ((currentMonth - 1) / 3) * 3 + 1;
                    LocalDate quarterStart = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
                    LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);
                    request.setStartDate(quarterStart);
                    request.setEndDate(quarterEnd);
                    break;
                case "all":
                    request.setStartDate(LocalDate.of(2020, 1, 1));
                    request.setEndDate(today.plusYears(1));
                    break;
                default:
                    break;
            }
        }

        // Fallback if dates are null
        if (request.getStartDate() == null) {
            request.setStartDate(today.withDayOfMonth(1));
        }
        if (request.getEndDate() == null) {
            request.setEndDate(today.withDayOfMonth(today.lengthOfMonth()));
        }

        // Validation: StartDate cannot be after EndDate
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }
    }

    /**
     * Retrieve complete revenue report data according to filters.
     */
    public RevenueReportData getReportData(RevenueReportRequest request) {
        resolveAndValidateDates(request);

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        RevenueReportData data = new RevenueReportData();
        data.setSummary(revenueReportDao.getSummary(start, end));
        data.setDailyItems(revenueReportDao.getDailyRevenue(start, end));
        data.setMovieItems(revenueReportDao.getMovieRevenue(start, end));
        data.setTransactionItems(revenueReportDao.getTransactions(start, end));

        return data;
    }

    /**
     * Generate downloadable CSV file with UTF-8 BOM for Microsoft Excel compatibility.
     */
    public byte[] generateCsvReport(RevenueReportRequest request) {
        resolveAndValidateDates(request);
        RevenueReportData data = getReportData(request);
        RevenueReportSummary summary = data.getSummary();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write UTF-8 BOM (\uFEFF) so Excel opens UTF-8 characters properly
        try {
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
        } catch (Exception ignored) {
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            DecimalFormat df = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.US));

            // 1. Report Header
            writer.println("BÁO CÁO DOANH THU RẠP CHIẾU PHIM - ROYALLY CINEMA");
            writer.println("Thời gian xuất báo cáo," + escape(LocalDateTime.now().format(DATETIME_FMT)));
            writer.println("Khoảng thời gian," + escape(request.getStartDate().format(DATE_FMT) + " đến " + request.getEndDate().format(DATE_FMT)));
            writer.println("Loại báo cáo," + escape(resolveReportTypeName(request.getReportType())));
            writer.println();

            // 2. Summary KPI Block
            writer.println("--- TỔNG QUAN CHỈ SỐ DOANH THU ---");
            writer.println("Tổng doanh thu toàn hệ thống (VNĐ)," + df.format(summary.getTotalRevenue()));
            writer.println("Doanh thu vé xem phim (VNĐ)," + df.format(summary.getTicketRevenue()));
            writer.println("Doanh thu bắp nước & F&B (VNĐ)," + df.format(summary.getFbRevenue()));
            writer.println("Tổng giá trị giảm giá/voucher (VNĐ)," + df.format(summary.getDiscountAmount()));
            writer.println("Tổng số vé đã bán," + summary.getTotalTickets());
            writer.println("Tổng số đơn đặt vé thành công," + summary.getTotalPaidBookings());
            writer.println();

            // 3. Detailed Data Table based on ReportType
            String type = request.getReportType() != null ? request.getReportType().toUpperCase().trim() : "DAILY";

            if ("MOVIE".equals(type)) {
                writer.println("--- CHI TIẾT DOANH THU THEO PHIM ---");
                writer.println("Mã Phim,Tên Phim,Số Suất Chiếu,Số Vé Đã Bán,Doanh Thu Vé (VNĐ),Doanh Thu F&B (VNĐ),Tổng Doanh Thu (VNĐ)");

                if (data.getMovieItems().isEmpty()) {
                    writer.println("Không có dữ liệu phim trong khoảng thời gian này,,,,,,");
                } else {
                    for (MovieRevenueItem m : data.getMovieItems()) {
                        writer.println(String.format(
                                "%d,%s,%d,%d,%s,%s,%s",
                                m.getMovieId(),
                                escape(m.getTitle()),
                                m.getShowtimesCount(),
                                m.getSoldTickets(),
                                df.format(m.getTicketRevenue()),
                                df.format(m.getFbRevenue()),
                                df.format(m.getTotalRevenue())
                        ));
                    }
                }
            } else if ("TRANSACTIONS".equals(type)) {
                writer.println("--- CHI TIẾT DANH SÁCH GIAO DỊCH / ĐƠN HÀNG ---");
                writer.println("Mã Đơn,Thời Gian Đặt,Khách Hàng,Số Điện Thoại,Phim,Phòng Chiếu,Suất Chiếu,Ghế Ngồi,Tiền Vé (VNĐ),Tiền F&B (VNĐ),Giảm Giá (VNĐ),Tổng Tiền (VNĐ),Hình Thức TT,Trạng Thái");

                if (data.getTransactionItems().isEmpty()) {
                    writer.println("Không có giao dịch thanh toán thành công trong khoảng thời gian này,,,,,,,,,,,,,");
                } else {
                    for (TransactionRevenueItem t : data.getTransactionItems()) {
                        writer.println(String.format(
                                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                escape(t.getBookingCode()),
                                escape(t.getBookingTime() != null ? t.getBookingTime().format(DATETIME_FMT) : "-"),
                                escape(t.getCustomerName()),
                                escape(t.getPhone()),
                                escape(t.getMovieTitle()),
                                escape(t.getRoomName()),
                                escape(t.getShowtimeStartTime() != null ? t.getShowtimeStartTime().format(DATETIME_FMT) : "-"),
                                escape(t.getSeatLabels()),
                                df.format(t.getTicketAmount()),
                                df.format(t.getFbAmount()),
                                df.format(t.getDiscountAmount()),
                                df.format(t.getTotalAmount()),
                                escape(t.getPaymentMethod()),
                                escape(t.getBookingStatus())
                        ));
                    }
                }
            } else {
                // Default: DAILY
                writer.println("--- CHI TIẾT DOANH THU THEO NGÀY ---");
                writer.println("Ngày,Doanh Thu Vé (VNĐ),Doanh Thu F&B (VNĐ),Giảm Giá (VNĐ),Tổng Doanh Thu (VNĐ),Số Vé Bán,Số Đơn Hàng");

                if (data.getDailyItems().isEmpty()) {
                    writer.println("Không có dữ liệu doanh thu trong khoảng thời gian này,,,,,,");
                } else {
                    for (DailyRevenueItem d : data.getDailyItems()) {
                        writer.println(String.format(
                                "%s,%s,%s,%s,%s,%d,%d",
                                escape(d.getDate() != null ? d.getDate().format(DATE_FMT) : "-"),
                                df.format(d.getTicketRevenue()),
                                df.format(d.getFbRevenue()),
                                df.format(d.getDiscountAmount()),
                                df.format(d.getTotalRevenue()),
                                d.getTicketCount(),
                                d.getBookingCount()
                        ));
                    }
                }
            }

            writer.flush();
        }

        return baos.toByteArray();
    }

    private String resolveReportTypeName(String type) {
        if ("MOVIE".equalsIgnoreCase(type)) return "Báo cáo doanh thu theo phim";
        if ("TRANSACTIONS".equalsIgnoreCase(type)) return "Báo cáo chi tiết giao dịch";
        return "Báo cáo doanh thu theo ngày";
    }

    private String escape(String value) {
        if (value == null) return "\"\"";
        String s = value.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
