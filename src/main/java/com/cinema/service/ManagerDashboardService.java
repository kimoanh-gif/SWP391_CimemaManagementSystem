package com.cinema.service;

import com.cinema.dao.ManagerDashboardDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.DashboardBooking;
import com.cinema.model.DashboardShowtime;
import com.cinema.model.DashboardSummary;
import com.cinema.model.ManagerHeaderInfo;
import com.cinema.model.RevenueChart;
import com.cinema.model.RevenueDay;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds everything shown on the Manager dashboard (M01). */
@Service
public class ManagerDashboardService {

    public static final List<String> RANGES = List.of("today", "7d", "month");

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.ENGLISH);

    private final ManagerDashboardDAO dashboardDAO;

    public ManagerDashboardService(ManagerDashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    public DashboardSummary getSummary() {
        return dashboardDAO.loadSummary(LocalDate.now());
    }

    public List<DashboardShowtime> getUpcomingShowtimes() {
        return dashboardDAO.findUpcomingShowtimes(5);
    }

    public List<DashboardBooking> getRecentBookings() {
        return dashboardDAO.findRecentBookings(6);
    }

    public ManagerHeaderInfo getHeaderInfo(AuthenticatedUser user) {
        int[] rooms = dashboardDAO.countRooms();
        return new ManagerHeaderInfo(rooms[0], rooms[1],
                user == null ? null : user.getFullName(),
                user == null ? null : user.getRole());
    }

    public RevenueChart getRevenueChart(String range) {
        String safeRange = RANGES.contains(range) ? range : "7d";
        LocalDate today = LocalDate.now();
        Map<String, RevenueDay> points = new LinkedHashMap<>();
        Map<String, Boolean> highlight = new LinkedHashMap<>();
        Map<String, String> tooltipPrefix = new LinkedHashMap<>();

        if ("today".equals(safeRange)) {
            Map<Integer, RevenueDay> byHour = dashboardDAO.findRevenueByHour(today);
            int currentHour = LocalDateTime.now().getHour();
            for (int hour = 8; hour <= 23; hour++) {
                String label = String.format("%02d", hour);
                points.put(label, byHour.getOrDefault(hour, new RevenueDay(today, null, null)));
                highlight.put(label, hour == currentHour);
                tooltipPrefix.put(label, label + ":00");
            }
        } else {
            LocalDate from = "month".equals(safeRange) ? today.withDayOfMonth(1) : today.minusDays(6);
            Map<LocalDate, RevenueDay> byDay = dashboardDAO.findRevenueByDay(from, today);
            for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
                String label;
                if (day.equals(today)) {
                    label = "Today";
                } else if ("month".equals(safeRange)) {
                    label = String.valueOf(day.getDayOfMonth());
                } else {
                    label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                }
                points.put(label, byDay.getOrDefault(day, new RevenueDay(day, null, null)));
                highlight.put(label, day.equals(today));
                tooltipPrefix.put(label, day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        + " " + day.format(DAY_MONTH));
            }
        }
        return buildChart(safeRange, points, highlight, tooltipPrefix);
    }

    static RevenueChart buildChart(String range, Map<String, RevenueDay> points,
                                   Map<String, Boolean> highlight, Map<String, String> tooltipPrefix) {
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal ticketTotal = BigDecimal.ZERO;
        for (RevenueDay point : points.values()) {
            max = max.max(point.getTicketRevenue()).max(point.getFnbRevenue());
            total = total.add(point.getTotal());
            ticketTotal = ticketTotal.add(point.getTicketRevenue());
        }
        double axisMax = niceCeiling(max.doubleValue());

        List<RevenueChart.Bar> bars = new ArrayList<>();
        for (Map.Entry<String, RevenueDay> entry : points.entrySet()) {
            RevenueDay point = entry.getValue();
            String tooltip = tooltipPrefix.get(entry.getKey()) + ": " + compact(point.getTotal().doubleValue()) + " ₫"
                    + " (tickets " + compact(point.getTicketRevenue().doubleValue())
                    + " • F&B " + compact(point.getFnbRevenue().doubleValue()) + ")";
            bars.add(new RevenueChart.Bar(entry.getKey(), tooltip,
                    percent(point.getTicketRevenue().doubleValue(), axisMax),
                    percent(point.getFnbRevenue().doubleValue(), axisMax),
                    Boolean.TRUE.equals(highlight.get(entry.getKey()))));
        }

        List<String> axis = new ArrayList<>();
        for (int step = 4; step >= 1; step--) axis.add(compact(axisMax * step / 4));
        axis.add("0");

        int ticketShare = total.signum() == 0 ? 0
                : ticketTotal.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
        return new RevenueChart(range, bars, axis, total, ticketShare);
    }

    /** Rounds up to 1, 2, 2.5 or 5 times a power of ten so the axis labels are readable. */
    static double niceCeiling(double value) {
        if (value <= 0) return 1_000_000;
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        for (double step : new double[]{1, 2, 2.5, 5, 10}) {
            if (value <= step * magnitude) return step * magnitude;
        }
        return 10 * magnitude;
    }

    /** 148650000 -> "148.65M", 25000 -> "25K". */
    static String compact(double value) {
        if (value >= 1_000_000_000) return trim(value / 1_000_000_000) + "B";
        if (value >= 1_000_000) return trim(value / 1_000_000) + "M";
        if (value >= 1_000) return trim(value / 1_000) + "K";
        return trim(value);
    }

    private static String trim(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static double percent(double value, double max) {
        if (max <= 0 || value <= 0) return 0;
        return Math.min(100, Math.round(value / max * 1000.0) / 10.0);
    }

    public String getTodayText() {
        return LocalDate.now().format(LONG_DATE);
    }

    public String getShiftText() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 8 && hour < 17) return "Day shift (08:00 - 17:00)";
        if (hour >= 17) return "Evening shift (17:00 - 24:00)";
        return "Night shift (00:00 - 08:00)";
    }
}
