package com.cinema.service;

import com.cinema.dao.RevenueReportDao;
import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.MovieRevenueItem;
import com.cinema.model.report.RevenueReportData;
import com.cinema.model.report.RevenueReportRequest;
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
import java.util.List;
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
        RevenueReportSummary summary = revenueReportDao.getSummary(start, end);
        List<DailyRevenueItem> dailyItems = revenueReportDao.getDailyRevenue(start, end);
        List<MovieRevenueItem> movieItems = revenueReportDao.getMovieRevenue(start, end);
        List<TransactionRevenueItem> transactionItems = revenueReportDao.getTransactions(start, end);

        if (summary == null) {
            summary = new RevenueReportSummary();
        }

        // Consistency safeguard: if daily records exist, ensure summary values reflect them
        if (!dailyItems.isEmpty()) {
            if (summary.getTotalRevenue() == null || summary.getTotalRevenue().compareTo(BigDecimal.ZERO) == 0
                    || summary.getTicketRevenue() == null || summary.getTicketRevenue().compareTo(BigDecimal.ZERO) == 0
                    || summary.getTotalTickets() == 0) {
                BigDecimal totalRev = BigDecimal.ZERO;
                BigDecimal ticketRev = BigDecimal.ZERO;
                BigDecimal fbRev = BigDecimal.ZERO;
                BigDecimal discount = BigDecimal.ZERO;
                int totalTickets = 0;
                int totalBookings = 0;
                for (DailyRevenueItem item : dailyItems) {
                    if (item.getTotalRevenue() != null) totalRev = totalRev.add(item.getTotalRevenue());
                    if (item.getTicketRevenue() != null) ticketRev = ticketRev.add(item.getTicketRevenue());
                    if (item.getFbRevenue() != null) fbRev = fbRev.add(item.getFbRevenue());
                    if (item.getDiscountAmount() != null) discount = discount.add(item.getDiscountAmount());
                    totalTickets += item.getTicketCount();
                    totalBookings += item.getBookingCount();
                }
                summary.setTotalRevenue(totalRev);
                summary.setTicketRevenue(ticketRev);
                summary.setFbRevenue(fbRev);
                summary.setDiscountAmount(discount);
                summary.setTotalTickets(totalTickets);
                summary.setTotalPaidBookings(totalBookings);
            }
        }

        data.setSummary(summary);
        data.setDailyItems(dailyItems);
        data.setMovieItems(movieItems);
        data.setTransactionItems(transactionItems);

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
            writer.println("Tổng doanh thu toàn hệ thống (VNĐ)," + formatMoney(summary.getTotalRevenue(), df));
            writer.println("Doanh thu vé xem phim (VNĐ)," + formatMoney(summary.getTicketRevenue(), df));
            writer.println("Doanh thu bắp nước & F&B (VNĐ)," + formatMoney(summary.getFbRevenue(), df));
            writer.println("Tổng giá trị giảm giá/voucher (VNĐ)," + formatMoney(summary.getDiscountAmount(), df));
            writer.println("Tổng số vé đã bán," + summary.getTotalTickets());
            writer.println("Tổng số đơn đặt vé thành công," + summary.getTotalPaidBookings());
            writer.println();

            // 3. Detailed Data Table based on ReportType
            String type = request.getReportType() != null ? request.getReportType().toUpperCase().trim() : "DAILY";

            if ("MOVIE".equals(type)) {
                writer.println("--- CHI TIẾT DOANH THU THEO PHIM ---");
                writer.println("Mã Phim,Tên Phim,Số Suất Chiếu,Số Vé Đã Bán,Doanh Thu Vé (VNĐ),Doanh Thu F&B (VNĐ),Tổng Doanh Thu (VNĐ)");

                if (data.getMovieItems().isEmpty()) {
                    writer.println(escape("Không có dữ liệu phim trong khoảng thời gian này") + ",,,,,,");
                } else {
                    for (MovieRevenueItem m : data.getMovieItems()) {
                        writer.println(String.format(
                                "%d,%s,%d,%d,%s,%s,%s",
                                m.getMovieId(),
                                escape(m.getTitle()),
                                m.getShowtimesCount(),
                                m.getSoldTickets(),
                                formatMoney(m.getTicketRevenue(), df),
                                formatMoney(m.getFbRevenue(), df),
                                formatMoney(m.getTotalRevenue(), df)
                        ));
                    }
                }
            } else if ("TRANSACTIONS".equals(type)) {
                writer.println("--- CHI TIẾT DANH SÁCH GIAO DỊCH / ĐƠN HÀNG ---");
                writer.println("Mã Đơn,Thời Gian Đặt,Khách Hàng,Số Điện Thoại,Phim,Phòng Chiếu,Suất Chiếu,Ghế Ngồi,Tiền Vé (VNĐ),Tiền F&B (VNĐ),Giảm Giá (VNĐ),Tổng Tiền (VNĐ),Hình Thức TT,Trạng Thái");

                if (data.getTransactionItems().isEmpty()) {
                    writer.println(escape("Không có giao dịch thanh toán thành công trong khoảng thời gian này") + ",,,,,,,,,,,,,");
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
                                formatMoney(t.getTicketAmount(), df),
                                formatMoney(t.getFbAmount(), df),
                                formatMoney(t.getDiscountAmount(), df),
                                formatMoney(t.getTotalAmount(), df),
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
                    writer.println(escape("Không có dữ liệu doanh thu trong khoảng thời gian này") + ",,,,,,");
                } else {
                    for (DailyRevenueItem d : data.getDailyItems()) {
                        writer.println(String.format(
                                "%s,%s,%s,%s,%s,%d,%d",
                                escape(d.getDate() != null ? d.getDate().format(DATE_FMT) : "-"),
                                formatMoney(d.getTicketRevenue(), df),
                                formatMoney(d.getFbRevenue(), df),
                                formatMoney(d.getDiscountAmount(), df),
                                formatMoney(d.getTotalRevenue(), df),
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

    private String formatMoney(BigDecimal amount, DecimalFormat df) {
        if (amount == null) return "\"0\"";
        return escape(df.format(amount));
    }

    private String escape(String value) {
        if (value == null) return "\"\"";
        String s = value.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
