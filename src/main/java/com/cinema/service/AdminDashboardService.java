package com.cinema.service;

import com.cinema.dao.AdminDashboardDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class AdminDashboardService {
    private final AdminDashboardDao dao;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public AdminDashboardService(AdminDashboardDao dao) {
        this(dao, Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
    }
    AdminDashboardService(AdminDashboardDao dao, Clock clock) { this.dao=dao; this.clock=clock; }

    @Transactional(readOnly = true)
    public Map<String, Object> load() {
        return load("day");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(String requestedPeriod) {
        return load(requestedPeriod, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(String requestedPeriod, String from, String to) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if ((fromDate == null) != (toDate == null)) {
            throw new IllegalArgumentException("Please select both the start date and end date.");
        }
        if (fromDate != null && (fromDate.isAfter(toDate) || toDate.isAfter(fromDate.plusYears(10)))) {
            throw new IllegalArgumentException("The start date must be on or before the end date. The maximum range is 10 years.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        Map<String,Object> data = new LinkedHashMap<>(dao.summary(today.atStartOfDay(),end));
        addRevenueChart(data, requestedPeriod, today, fromDate, toDate);
        data.put("topMovies",dao.topMovies(today.minusDays(6).atStartOfDay(),end));
        data.put("recent",dao.recentBookings());
        data.put("upcoming",dao.upcoming(now,end));
        data.put("updatedAt",now);
        data.put("today",today);
        return data;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
            LocalDate date = LocalDate.parse(value);
            if (date.getYear() < 1900 || date.getYear() > 9998) throw new IllegalArgumentException();
            return date;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid date. Please select a date between 1900 and 9998.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loadDateRange(String from, String to) {
        if (from == null && to == null) {
            LocalDate today = LocalDate.now(clock);
            from = today.minusDays(6).toString();
            to = today.toString();
        }
        return loadView(null, "custom", from, to);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loadFiltered(String period, String range, String from, String to) {
        LocalDate today = LocalDate.now(clock);
        Map<String, LocalDate> starts = new LinkedHashMap<>();
        starts.put("last7", today.minusDays(6));
        starts.put("last30", today.minusDays(29));
        starts.put("month", today.withDayOfMonth(1));
        starts.put("quarter", LocalDate.of(today.getYear(), (today.getMonthValue() - 1) / 3 * 3 + 1, 1));
        starts.put("year", today.withDayOfYear(1));
        String selected = "custom".equals(range) || starts.containsKey(range) ? range : "last7";
        if ("custom".equals(selected) && (from == null || from.isBlank() || to == null || to.isBlank())) {
            throw new IllegalArgumentException("Please select both the start date and end date.");
        }
        Map<String, Object> data = load(period,
                "custom".equals(selected) ? from : starts.get(selected).toString(),
                "custom".equals(selected) ? to : today.toString());
        data.put("range", selected);
        data.put("rangeStarts", starts);
        String unit = switch ((String) data.get("period")) {
            case "week" -> "week (Monday–Sunday)";
            case "month" -> "month";
            case "quarter" -> "quarter";
            case "year" -> "year";
            default -> "day";
        };
        var format = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        data.put("chartDescription", "Revenue from " + ((LocalDate) data.get("chartStart")).format(format)
                + " to " + ((LocalDate) data.get("chartEnd")).format(format) + " · Each bar represents one " + unit);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loadView(String period, String mode, String from, String to) {
        boolean custom = "custom".equals(mode);
        Map<String, Object> data;
        if (custom) {
            LocalDate start = parseDate(from);
            LocalDate end = parseDate(to);
            if (start == null || end == null) {
                throw new IllegalArgumentException("Please select both the start date and end date.");
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            String grouping = days <= 31 ? "day" : days <= 90 ? "week"
                    : !end.isAfter(start.plusYears(2).minusDays(1)) ? "month"
                    : !end.isAfter(start.plusYears(5).minusDays(1)) ? "quarter" : "year";
            data = loadFiltered(grouping, "custom", from, to);
        } else {
            data = load(period);
            data.put("range", "quick");
        }
        LocalDate today = (LocalDate) data.get("today");
        LocalDate yearStart = today.withDayOfYear(1);
        Map<String, Map<String, LocalDate>> quickRanges = new LinkedHashMap<>();
        quickRanges.put("day", Map.of("start", today.minusDays(6), "end", today));
        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        quickRanges.put("week", Map.of("start", monday.minusWeeks(7), "end", monday.plusDays(6)));
        quickRanges.put("month", Map.of("start", yearStart, "end", yearStart.plusYears(1).minusDays(1)));
        quickRanges.put("quarter", quickRanges.get("month"));
        quickRanges.put("year", Map.of("start", yearStart.minusYears(4), "end", yearStart.plusYears(1).minusDays(1)));
        data.put("quickRanges", quickRanges);
        var format = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String unit = switch ((String) data.get("period")) {
            case "week" -> "week";
            case "month" -> "month";
            case "quarter" -> "quarter";
            case "year" -> "year";
            default -> "day";
        };
        data.put("chartDescription", ((LocalDate) data.get("chartStart")).format(format) + " – "
                + ((LocalDate) data.get("chartEnd")).format(format) + " · "
                + (custom ? "Automatically grouped" : "Grouped") + " by " + unit
                + " · Revenue counted only within the range and through today");
        return data;
    }

    private void addRevenueChart(Map<String, Object> data, String requestedPeriod, LocalDate today,
                                 LocalDate fromDate, LocalDate toDate) {
        String period = Set.of("day", "week", "month", "quarter", "year").contains(requestedPeriod == null ? "" : requestedPeriod)
                ? requestedPeriod : "day";
        LocalDate start;
        int count;
        String description;
        switch (period) {
            case "week" -> {
                start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(7);
                count = 8;
                description = "Last 8 weeks · Monday–Sunday";
            }
            case "month" -> { start = today.withDayOfYear(1); count = 12; description = "12 months of " + today.getYear(); }
            case "quarter" -> { start = today.withDayOfYear(1); count = 4; description = "4 quarters of " + today.getYear(); }
            case "year" -> { start = today.withDayOfYear(1).minusYears(4); count = 5; description = "Last 5 years"; }
            default -> { start = today.minusDays(6); count = 7; description = "Last 7 days"; }
        }
        List<Map<String, Object>> bars = new ArrayList<>();
        boolean custom = fromDate != null;
        LocalDate rangeEnd = null;
        Map<LocalDate, BigDecimal> dailyAmounts = new HashMap<>();
        if (custom) {
            start = fromDate;
            rangeEnd = toDate.plusDays(1);
            description = "Revenue for the selected date range · Weeks start on Monday";
            LocalDate queryEnd = rangeEnd.isAfter(today.plusDays(1)) ? today.plusDays(1) : rangeEnd;
            if (start.isBefore(queryEnd)) {
                for (Map<String, Object> row : dao.daily(start.atStartOfDay(), queryEnd.atStartOfDay())) {
                    dailyAmounts.put(((java.sql.Date) row.get("day")).toLocalDate(), (BigDecimal) row.get("amount"));
                }
            }
        }
        LocalDate cursor = start;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        for (int i = 0; custom ? cursor.isBefore(rangeEnd) : i < count; i++) {
            LocalDate bucketStart = custom ? switch (period) {
                case "week" -> cursor.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case "month" -> cursor.withDayOfMonth(1);
                case "quarter" -> LocalDate.of(cursor.getYear(), ((cursor.getMonthValue() - 1) / 3) * 3 + 1, 1);
                case "year" -> cursor.withDayOfYear(1);
                default -> cursor;
            } : cursor;
            LocalDate next = switch (period) {
                case "week" -> bucketStart.plusWeeks(1);
                case "month" -> bucketStart.plusMonths(1);
                case "quarter" -> bucketStart.plusMonths(3);
                case "year" -> bucketStart.plusYears(1);
                default -> cursor.plusDays(1);
            };
            if (custom && next.isAfter(rangeEnd)) next = rangeEnd;
            // Never include future-dated payments in the current or a future bucket.
            LocalDate queryEnd = next.isAfter(today.plusDays(1)) ? today.plusDays(1) : next;
            BigDecimal amount = BigDecimal.ZERO;
            if (custom) {
                for (LocalDate date = cursor; date.isBefore(next); date = date.plusDays(1)) {
                    amount = amount.add(dailyAmounts.getOrDefault(date, BigDecimal.ZERO));
                }
            } else if (!cursor.isAfter(today)) {
                amount = dao.revenue(cursor.atStartOfDay(), queryEnd.atStartOfDay());
            }
            String label = switch (period) {
                case "month" -> "M" + cursor.getMonthValue() + "/" + cursor.getYear();
                case "quarter" -> "Q" + ((cursor.getMonthValue() - 1) / 3 + 1) + "/" + cursor.getYear();
                case "year" -> String.valueOf(cursor.getYear());
                default -> cursor.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"));
            };
            Map<String, Object> bar = new LinkedHashMap<>();
            bar.put("label", label);
            bar.put("start", cursor);
            bar.put("end", next.minusDays(1));
            bar.put("amount", amount);
            bars.add(bar);
            total = total.add(amount);
            max = max.max(amount);
            cursor = next;
        }
        for (Map<String, Object> bar : bars) {
            BigDecimal amount = (BigDecimal) bar.get("amount");
            bar.put("height", max.signum() == 0 ? BigDecimal.ZERO
                    : amount.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP));
            bar.put("peak", amount.signum() > 0 && amount.compareTo(max) == 0);
        }
        data.put("period", period);
        data.put("chartDescription", description);
        data.put("chartStart", start);
        data.put("chartEnd", cursor.minusDays(1));
        data.put("chartBars", bars);
        data.put("chartTotal", total);
    }
}
