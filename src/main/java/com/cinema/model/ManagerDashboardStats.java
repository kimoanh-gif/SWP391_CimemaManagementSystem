package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing live operational summary metrics for Screen 53: Manager Dashboard.
 * All metrics are strictly computed from real database records without mock/fake data.
 */
public class ManagerDashboardStats {

    private int totalMovies;
    private int nowShowingMovies;
    private int upcomingMovies;
    private int totalGenres;
    private int totalRooms;
    private int activeRooms;
    private int todayShowtimesCount;
    private int totalSeatTypes;
    private int totalPricingRules;
    private BigDecimal baseTicketPrice;
    private BigDecimal averageTicketPrice;
    private int totalFBItems;
    private int activeFBItems;

    // Real transaction and operational performance metrics
    private boolean hasTransactionData;
    private BigDecimal totalRevenue;
    private BigDecimal ticketRevenue;
    private BigDecimal fbRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal weeklyRevenue;
    private BigDecimal weeklyTicketRevenue;
    private BigDecimal weeklyFbRevenue;
    private int boxOfficePercent;
    private int concessionsPercent;
    private Double seatOccupancyRate;
    private int totalBookedTickets;
    private int activePromotionsCount;
    private String selectedPeriod;
    private BigDecimal periodRevenue;
    private String periodUnitLabel;

    private List<DailyRevenueChartItem> weeklyChartBars = new ArrayList<>();
    private List<RecentBookingItem> recentBookings = new ArrayList<>();

    public ManagerDashboardStats() {
        this.baseTicketPrice = BigDecimal.ZERO;
        this.averageTicketPrice = BigDecimal.ZERO;
        this.totalRevenue = BigDecimal.ZERO;
        this.ticketRevenue = BigDecimal.ZERO;
        this.fbRevenue = BigDecimal.ZERO;
        this.todayRevenue = BigDecimal.ZERO;
        this.weeklyRevenue = BigDecimal.ZERO;
        this.weeklyTicketRevenue = BigDecimal.ZERO;
        this.weeklyFbRevenue = BigDecimal.ZERO;
        this.boxOfficePercent = 0;
        this.concessionsPercent = 0;
        this.hasTransactionData = false;
        this.seatOccupancyRate = null;
        this.activePromotionsCount = 0;
        this.selectedPeriod = "last7days";
        this.periodRevenue = BigDecimal.ZERO;
        this.periodUnitLabel = "₫ / 7 days";
    }

    public static class DailyRevenueChartItem {
        private String dayLabel;
        private LocalDate date;
        private boolean today;
        private BigDecimal ticketRevenue = BigDecimal.ZERO;
        private BigDecimal fbRevenue = BigDecimal.ZERO;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private int ticketBarHeight = 0;
        private int fbBarHeight = 0;
        private int ticketBarY = 156;
        private int fbBarY = 156;
        private int rectX = 74;

        public String getDayLabel() { return dayLabel; }
        public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public boolean isToday() { return today; }
        public void setToday(boolean today) { this.today = today; }

        public BigDecimal getTicketRevenue() { return ticketRevenue; }
        public void setTicketRevenue(BigDecimal ticketRevenue) { this.ticketRevenue = ticketRevenue; }

        public BigDecimal getFbRevenue() { return fbRevenue; }
        public void setFbRevenue(BigDecimal fbRevenue) { this.fbRevenue = fbRevenue; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

        public int getTicketBarHeight() { return ticketBarHeight; }
        public void setTicketBarHeight(int ticketBarHeight) { this.ticketBarHeight = ticketBarHeight; }

        public int getFbBarHeight() { return fbBarHeight; }
        public void setFbBarHeight(int fbBarHeight) { this.fbBarHeight = fbBarHeight; }

        public int getTicketBarY() { return ticketBarY; }
        public void setTicketBarY(int ticketBarY) { this.ticketBarY = ticketBarY; }

        public int getFbBarY() { return fbBarY; }
        public void setFbBarY(int fbBarY) { this.fbBarY = fbBarY; }

        public int getRectX() { return rectX; }
        public void setRectX(int rectX) { this.rectX = rectX; }
    }

    public static class RecentBookingItem {
        private int bookingId;
        private String bookingCode;
        private LocalDateTime bookingTime;
        private String customerName;
        private String phone;
        private String movieTitle;
        private String roomName;
        private LocalDateTime showtimeStart;
        private String status;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        public int getBookingId() { return bookingId; }
        public void setBookingId(int bookingId) { this.bookingId = bookingId; }

        public String getBookingCode() { return bookingCode; }
        public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

        public LocalDateTime getBookingTime() { return bookingTime; }
        public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getMovieTitle() { return movieTitle; }
        public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }

        public LocalDateTime getShowtimeStart() { return showtimeStart; }
        public void setShowtimeStart(LocalDateTime showtimeStart) { this.showtimeStart = showtimeStart; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    // Getters and Setters
    public int getTotalMovies() { return totalMovies; }
    public void setTotalMovies(int totalMovies) { this.totalMovies = totalMovies; }

    public int getNowShowingMovies() { return nowShowingMovies; }
    public void setNowShowingMovies(int nowShowingMovies) { this.nowShowingMovies = nowShowingMovies; }

    public int getUpcomingMovies() { return upcomingMovies; }
    public void setUpcomingMovies(int upcomingMovies) { this.upcomingMovies = upcomingMovies; }

    public int getTotalGenres() { return totalGenres; }
    public void setTotalGenres(int totalGenres) { this.totalGenres = totalGenres; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    public int getActiveRooms() { return activeRooms; }
    public void setActiveRooms(int activeRooms) { this.activeRooms = activeRooms; }

    public int getTodayShowtimesCount() { return todayShowtimesCount; }
    public void setTodayShowtimesCount(int todayShowtimesCount) { this.todayShowtimesCount = todayShowtimesCount; }

    public int getTotalSeatTypes() { return totalSeatTypes; }
    public void setTotalSeatTypes(int totalSeatTypes) { this.totalSeatTypes = totalSeatTypes; }

    public int getTotalPricingRules() { return totalPricingRules; }
    public void setTotalPricingRules(int totalPricingRules) { this.totalPricingRules = totalPricingRules; }

    public BigDecimal getBaseTicketPrice() { return baseTicketPrice; }
    public void setBaseTicketPrice(BigDecimal baseTicketPrice) { this.baseTicketPrice = baseTicketPrice; }

    public BigDecimal getAverageTicketPrice() { return averageTicketPrice; }
    public void setAverageTicketPrice(BigDecimal averageTicketPrice) { this.averageTicketPrice = averageTicketPrice; }

    public int getTotalFBItems() { return totalFBItems; }
    public void setTotalFBItems(int totalFBItems) { this.totalFBItems = totalFBItems; }

    public int getActiveFBItems() { return activeFBItems; }
    public void setActiveFBItems(int activeFBItems) { this.activeFBItems = activeFBItems; }

    public boolean isHasTransactionData() { return hasTransactionData; }
    public void setHasTransactionData(boolean hasTransactionData) { this.hasTransactionData = hasTransactionData; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getTicketRevenue() { return ticketRevenue; }
    public void setTicketRevenue(BigDecimal ticketRevenue) { this.ticketRevenue = ticketRevenue; }

    public BigDecimal getFbRevenue() { return fbRevenue; }
    public void setFbRevenue(BigDecimal fbRevenue) { this.fbRevenue = fbRevenue; }

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }

    public BigDecimal getWeeklyRevenue() { return weeklyRevenue; }
    public void setWeeklyRevenue(BigDecimal weeklyRevenue) { this.weeklyRevenue = weeklyRevenue; }

    public BigDecimal getWeeklyTicketRevenue() { return weeklyTicketRevenue; }
    public void setWeeklyTicketRevenue(BigDecimal weeklyTicketRevenue) { this.weeklyTicketRevenue = weeklyTicketRevenue; }

    public BigDecimal getWeeklyFbRevenue() { return weeklyFbRevenue; }
    public void setWeeklyFbRevenue(BigDecimal weeklyFbRevenue) { this.weeklyFbRevenue = weeklyFbRevenue; }

    public int getBoxOfficePercent() { return boxOfficePercent; }
    public void setBoxOfficePercent(int boxOfficePercent) { this.boxOfficePercent = boxOfficePercent; }

    public int getConcessionsPercent() { return concessionsPercent; }
    public void setConcessionsPercent(int concessionsPercent) { this.concessionsPercent = concessionsPercent; }

    public Double getSeatOccupancyRate() { return seatOccupancyRate; }
    public void setSeatOccupancyRate(Double seatOccupancyRate) { this.seatOccupancyRate = seatOccupancyRate; }

    public int getTotalBookedTickets() { return totalBookedTickets; }
    public void setTotalBookedTickets(int totalBookedTickets) { this.totalBookedTickets = totalBookedTickets; }

    public int getActivePromotionsCount() { return activePromotionsCount; }
    public void setActivePromotionsCount(int activePromotionsCount) { this.activePromotionsCount = activePromotionsCount; }

    public String getSelectedPeriod() { return selectedPeriod; }
    public void setSelectedPeriod(String selectedPeriod) { this.selectedPeriod = selectedPeriod; }

    public BigDecimal getPeriodRevenue() { return periodRevenue; }
    public void setPeriodRevenue(BigDecimal periodRevenue) { this.periodRevenue = periodRevenue; }

    public String getPeriodUnitLabel() { return periodUnitLabel; }
    public void setPeriodUnitLabel(String periodUnitLabel) { this.periodUnitLabel = periodUnitLabel; }

    public List<DailyRevenueChartItem> getWeeklyChartBars() { return weeklyChartBars; }
    public void setWeeklyChartBars(List<DailyRevenueChartItem> weeklyChartBars) { this.weeklyChartBars = weeklyChartBars; }

    public List<RecentBookingItem> getRecentBookings() { return recentBookings; }
    public void setRecentBookings(List<RecentBookingItem> recentBookings) { this.recentBookings = recentBookings; }
}
