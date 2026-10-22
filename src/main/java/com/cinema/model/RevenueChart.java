package com.cinema.model;

import java.math.BigDecimal;
import java.util.List;

/** Pre-computed bar chart for the "Revenue Overview" card on the Manager dashboard. */
public class RevenueChart {

    /** One group of two bars (ticket + F&B). Heights are percentages of the chart height. */
    public static class Bar {
        private final String label;
        private final String tooltip;
        private final double ticketHeight;
        private final double fnbHeight;
        private final boolean today;

        public Bar(String label, String tooltip, double ticketHeight, double fnbHeight, boolean today) {
            this.label = label;
            this.tooltip = tooltip;
            this.ticketHeight = ticketHeight;
            this.fnbHeight = fnbHeight;
            this.today = today;
        }

        public String getLabel() { return label; }
        public String getTooltip() { return tooltip; }
        public double getTicketHeight() { return ticketHeight; }
        public double getFnbHeight() { return fnbHeight; }
        public boolean isToday() { return today; }
    }

    private final String range;
    private final List<Bar> bars;
    private final List<String> axisLabels;
    private final BigDecimal total;
    private final int ticketSharePercent;

    public RevenueChart(String range, List<Bar> bars, List<String> axisLabels,
                        BigDecimal total, int ticketSharePercent) {
        this.range = range;
        this.bars = bars;
        this.axisLabels = axisLabels;
        this.total = total;
        this.ticketSharePercent = ticketSharePercent;
    }

    public String getRange() { return range; }
    public List<Bar> getBars() { return bars; }
    /** Y axis labels from top to bottom (the last one is always "0"). */
    public List<String> getAxisLabels() { return axisLabels; }
    public BigDecimal getTotal() { return total; }
    public int getTicketSharePercent() { return ticketSharePercent; }
    public int getFnbSharePercent() { return total.signum() == 0 ? 0 : 100 - ticketSharePercent; }
    public boolean isEmpty() { return total.signum() == 0; }

    public String getTotalSuffix() {
        return switch (range) {
            case "today" -> "today";
            case "month" -> "this month";
            default -> "last 7 days";
        };
    }
}
