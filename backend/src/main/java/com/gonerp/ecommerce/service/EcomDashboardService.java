package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomDashboardResponse;
import com.gonerp.ecommerce.model.EcomEtsyTransaction;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.EcomStore;
import com.gonerp.ecommerce.model.enums.SalesChannel;
import com.gonerp.ecommerce.model.enums.StoreRole;
import com.gonerp.ecommerce.repository.EcomEtsyTransactionRepository;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import com.gonerp.ecommerce.repository.EcomStoreRepository;
import com.gonerp.finance.repository.FinanceCurrencyRateRepository;
import com.gonerp.organization.model.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EcomDashboardService {

    private static final String DASHBOARD_CURRENCY = "EUR";

    private final EcomOrderRepository orderRepository;
    private final EcomEtsyTransactionRepository txnRepository;
    private final EcomStoreRepository storeRepository;
    private final FinanceCurrencyRateRepository rateRepository;
    private final EcomAccessService accessService;

    /**
     * Etsy dashboard for a single store or combined (storeId=null = all Etsy stores).
     * All amounts converted to EUR for the global view.
     */
    private static final StoreRole[] DASHBOARD_ROLES = {
            StoreRole.STORE_ADMIN, StoreRole.SELLER, StoreRole.SELLER_SUPPORT, StoreRole.FULFILLMENT_STAFF
    };

    public EcomDashboardResponse getDashboard(Long storeId, LocalDate startDate, LocalDate endDate) {
        accessService.requireEcommerceAccess();
        if (storeId != null) {
            accessService.requireStoreRole(storeId, DASHBOARD_ROLES);
        }
        Organization org = accessService.resolveOrganization();

        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.atTime(LocalTime.MAX);

        // Load orders — filter to Etsy only
        List<EcomOrder> orders;
        List<Long> etsyStoreIds;
        if (storeId != null) {
            orders = orderRepository.findByStoreIdAndOrderDateBetweenOrderByOrderDateDesc(storeId, startDt, endDt);
            etsyStoreIds = List.of(storeId);
        } else {
            // All Etsy stores in this org
            List<EcomStore> etsyStores = storeRepository.findByOrganizationIdAndActiveOrderByNameAsc(org.getId(), true)
                    .stream().filter(s -> s.getSalesChannel() == SalesChannel.ETSY).toList();
            etsyStoreIds = etsyStores.stream().map(EcomStore::getId).toList();
            orders = orderRepository.findByOrganizationIdAndOrderDateBetweenOrderByOrderDateDesc(org.getId(), startDt, endDt)
                    .stream().filter(o -> o.getSalesChannel() == SalesChannel.ETSY).toList();
        }

        // Build currency conversion cache
        Map<String, BigDecimal> rateCache = new HashMap<>();
        rateCache.put(DASHBOARD_CURRENCY, BigDecimal.ONE);

        // Load Etsy transactions
        List<EcomEtsyTransaction> txns;
        if (storeId != null) {
            txns = txnRepository.findByStoreIdAndTxnDateBetweenOrderByTxnDateDesc(storeId, startDate, endDate);
        } else {
            txns = etsyStoreIds.isEmpty() ? List.of() : txnRepository.findByStoreIdsAndDateRange(etsyStoreIds, startDate, endDate);
        }

        // === Order metrics ===
        int totalOrders = orders.size();
        int fulfilledOrders = 0, pendingOrders = 0, refundedOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        BigDecimal totalEarning = BigDecimal.ZERO;
        BigDecimal totalFulfillment = BigDecimal.ZERO;
        BigDecimal totalOther = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal pendingRevenue = BigDecimal.ZERO;
        BigDecimal pendingEarning = BigDecimal.ZERO;

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();
        Map<LocalDate, BigDecimal> dailyProfit = new TreeMap<>();

        for (EcomOrder o : orders) {
            BigDecimal rate = getRate(o.getCurrency(), org.getId(), rateCache);

            BigDecimal rev = convert(o.getOrderTotal(), rate);
            totalRevenue = totalRevenue.add(rev);

            if (o.getPlatformFee() != null) totalPlatformFee = totalPlatformFee.add(convert(o.getPlatformFee(), rate));
            if (o.getRefundAmount() != null) totalRefundAmount = totalRefundAmount.add(convert(o.getRefundAmount(), rate));
            if (o.getEarningAfterPlatformFee() != null) totalEarning = totalEarning.add(convert(o.getEarningAfterPlatformFee(), rate));

            if (o.getFulfillmentCost() != null && o.getFulfillmentCost().compareTo(BigDecimal.ZERO) > 0) {
                fulfilledOrders++;
                totalFulfillment = totalFulfillment.add(convert(o.getFulfillmentCost(), rate));
                if (o.getOtherCost() != null) totalOther = totalOther.add(convert(o.getOtherCost(), rate));
                if (o.getGrossProfit() != null) totalProfit = totalProfit.add(convert(o.getGrossProfit(), rate));
            } else {
                pendingOrders++;
                pendingRevenue = pendingRevenue.add(rev);
                if (o.getEarningAfterPlatformFee() != null) pendingEarning = pendingEarning.add(convert(o.getEarningAfterPlatformFee(), rate));
            }

            if (o.isRefunded()) refundedOrders++;

            String status = o.getStatus() != null ? o.getStatus().name() : "UNKNOWN";
            statusCounts.merge(status, 1, Integer::sum);

            if (o.getOrderDate() != null) {
                LocalDate day = o.getOrderDate().toLocalDate();
                dailyRevenue.merge(day, rev, BigDecimal::add);
                BigDecimal profit = o.getGrossProfit() != null ? convert(o.getGrossProfit(), rate) : BigDecimal.ZERO;
                dailyProfit.merge(day, profit, BigDecimal::add);
            }
        }

        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgRevenue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgProfit = fulfilledOrders > 0
                ? totalProfit.divide(BigDecimal.valueOf(fulfilledOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // === Etsy balance from transactions ===
        BigDecimal etsySales = BigDecimal.ZERO, etsyFees = BigDecimal.ZERO, etsyRefunds = BigDecimal.ZERO;
        BigDecimal etsyAds = BigDecimal.ZERO, etsyListingFees = BigDecimal.ZERO, etsyDeposits = BigDecimal.ZERO;

        for (EcomEtsyTransaction t : txns) {
            BigDecimal txnRate = getRate(t.getCurrency(), org.getId(), rateCache);
            BigDecimal amt = convert(t.getAmount(), txnRate);
            BigDecimal fees = convert(t.getFeesAndTaxes(), txnRate);
            BigDecimal net = convert(t.getNet(), txnRate);

            switch (t.getType()) {
                case "Sale" -> etsySales = etsySales.add(amt);
                case "Fee" -> {
                    String title = t.getTitle() != null ? t.getTitle().toLowerCase() : "";
                    if (title.contains("listing fee")) {
                        etsyListingFees = etsyListingFees.add(net);
                    } else {
                        etsyFees = etsyFees.add(fees);
                    }
                }
                case "Tax" -> etsyFees = etsyFees.add(fees);
                case "Refund" -> etsyRefunds = etsyRefunds.add(amt);
                case "Marketing" -> etsyAds = etsyAds.add(net);
                case "Deposit" -> etsyDeposits = etsyDeposits.add(amt);
            }
        }
        BigDecimal etsyNet = etsySales.add(etsyFees).add(etsyRefunds).add(etsyAds).add(etsyListingFees).subtract(etsyDeposits);

        // === Build response ===
        List<Map<String, Object>> statusList = statusCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("status", e.getKey(), "count", e.getValue()))
                .toList();

        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        Set<LocalDate> allDays = new TreeSet<>();
        allDays.addAll(dailyRevenue.keySet());
        allDays.addAll(dailyProfit.keySet());
        for (LocalDate day : allDays) {
            dailyTrend.add(Map.of(
                    "date", day.toString(),
                    "revenue", dailyRevenue.getOrDefault(day, BigDecimal.ZERO),
                    "profit", dailyProfit.getOrDefault(day, BigDecimal.ZERO)
            ));
        }

        List<Map<String, Object>> profitBreakdown = List.of(
                Map.of("label", "Revenue", "value", totalRevenue),
                Map.of("label", "Platform Fees", "value", totalPlatformFee.negate()),
                Map.of("label", "Refunds", "value", totalRefundAmount.negate()),
                Map.of("label", "Fulfillment", "value", totalFulfillment.negate()),
                Map.of("label", "Other Costs", "value", totalOther.negate()),
                Map.of("label", "Gross Profit", "value", totalProfit)
        );

        return EcomDashboardResponse.builder()
                .currency(DASHBOARD_CURRENCY)
                .totalOrders(totalOrders)
                .fulfilledOrders(fulfilledOrders)
                .pendingFulfillmentOrders(pendingOrders)
                .refundedOrders(refundedOrders)
                .totalRevenue(totalRevenue)
                .totalPlatformFee(totalPlatformFee)
                .totalRefundAmount(totalRefundAmount)
                .totalEarningAfterFees(totalEarning)
                .totalFulfillmentCost(totalFulfillment)
                .totalOtherCost(totalOther)
                .totalGrossProfit(totalProfit)
                .profitMarginPct(profitMargin)
                .avgRevenuePerOrder(avgRevenue)
                .avgProfitPerOrder(avgProfit)
                .pendingFulfillmentRevenue(pendingRevenue)
                .pendingFulfillmentEarning(pendingEarning)
                .etsySales(etsySales)
                .etsyFees(etsyFees)
                .etsyRefunds(etsyRefunds)
                .etsyAds(etsyAds)
                .etsyListingFees(etsyListingFees)
                .etsyDeposits(etsyDeposits)
                .etsyNetBalance(etsyNet)
                .ordersByStatus(statusList)
                .dailyTrend(dailyTrend)
                .profitBreakdown(profitBreakdown)
                .build();
    }

    private BigDecimal getRate(String fromCurrency, Long orgId, Map<String, BigDecimal> cache) {
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(DASHBOARD_CURRENCY)) return BigDecimal.ONE;
        String key = fromCurrency.toUpperCase();
        return cache.computeIfAbsent(key, k -> {
            try {
                return rateRepository.findRateForDate(orgId, k, DASHBOARD_CURRENCY, LocalDate.now())
                        .map(r -> r.getRate())
                        .orElse(BigDecimal.ONE);
            } catch (Exception e) {
                log.warn("No exchange rate for {} -> {}", k, DASHBOARD_CURRENCY);
                return BigDecimal.ONE;
            }
        });
    }

    private BigDecimal convert(BigDecimal amount, BigDecimal rate) {
        if (amount == null) return BigDecimal.ZERO;
        if (rate == null || rate.equals(BigDecimal.ONE)) return amount;
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
