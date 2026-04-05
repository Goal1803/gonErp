package com.gonerp.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EcomDashboardResponse {

    private String currency;

    // Summary cards
    private int totalOrders;
    private int fulfilledOrders;
    private int pendingFulfillmentOrders;
    private int refundedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalPlatformFee;
    private BigDecimal totalRefundAmount;
    private BigDecimal totalEarningAfterFees;
    private BigDecimal totalFulfillmentCost;
    private BigDecimal totalOtherCost;
    private BigDecimal totalGrossProfit;
    private BigDecimal profitMarginPct;
    private BigDecimal avgRevenuePerOrder;
    private BigDecimal avgProfitPerOrder;

    // Pending fulfillment value (orders without fulfillment cost)
    private BigDecimal pendingFulfillmentRevenue;
    private BigDecimal pendingFulfillmentEarning;

    // Etsy balance (from statement transactions)
    private BigDecimal etsySales;
    private BigDecimal etsyFees;
    private BigDecimal etsyRefunds;
    private BigDecimal etsyAds;
    private BigDecimal etsyListingFees;
    private BigDecimal etsyDeposits;
    private BigDecimal etsyNetBalance;

    // Orders by status
    private List<Map<String, Object>> ordersByStatus;

    // Daily revenue + profit (for line chart)
    private List<Map<String, Object>> dailyTrend;

    // Profit breakdown (for waterfall chart)
    private List<Map<String, Object>> profitBreakdown;
}
