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
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        Map<String,Object> data = new LinkedHashMap<>(dao.summary(today.atStartOfDay(),end));
        List<Map<String,Object>> raw = dao.daily(today.minusDays(6).atStartOfDay(),end);
        Map<LocalDate,BigDecimal> byDate = new HashMap<>();
        raw.forEach(row -> byDate.put(((java.sql.Date) row.get("day")).toLocalDate(), (BigDecimal)row.get("amount")));
        BigDecimal max = byDate.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<Map<String,Object>> days = new ArrayList<>();
        for(int i=6;i>=0;i--) {
            LocalDate day=today.minusDays(i);
            BigDecimal amount=byDate.getOrDefault(day,BigDecimal.ZERO);
            BigDecimal height=max.signum()==0?BigDecimal.ZERO:amount.multiply(BigDecimal.valueOf(100)).divide(max,2,RoundingMode.HALF_UP);
            days.add(Map.of("day",day,"amount",amount,"height",height,"peak",amount.signum()>0 && amount.compareTo(max)==0));
        }
        data.put("days",days);
        data.put("weekTotal",byDate.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add));
        data.put("topMovies",dao.topMovies(today.minusDays(6).atStartOfDay(),end));
        data.put("recent",dao.recentBookings());
        data.put("upcoming",dao.upcoming(now,end));
        data.put("updatedAt",now);
        data.put("startDate",today.minusDays(6));
        data.put("today",today);
        return data;
    }
}
