package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.SkuPriceEntity;
import com.ecomanalyser.domain.MergedOrderPaymentEntity;
import com.ecomanalyser.dto.ChartResponse;
import com.ecomanalyser.dto.TimeSeriesPoint;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
import com.ecomanalyser.repository.MergedOrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuPriceRepository skuPriceRepository;
    private final MergedOrderPaymentRepository mergedOrderRepository;
    private final ApplicationContext applicationContext;

    public enum Aggregation { DAY, MONTH, YEAR, QUARTER }

    public ChartResponse<TimeSeriesPoint> ordersByTime(LocalDate start, LocalDate end, Aggregation agg) {
        List<MergedOrderPaymentEntity> orders = mergedOrderRepository.findByOrderDateBetween(start, end);
        Map<LocalDate, BigDecimal> counts = new TreeMap<>();
        for (MergedOrderPaymentEntity o : orders) {
            LocalDate key = aggregateDate(o.getOrderDate(), agg);
            counts.merge(key, BigDecimal.ONE, BigDecimal::add);
        }
        List<TimeSeriesPoint> points = counts.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }

    public ChartResponse<TimeSeriesPoint> paymentsByTime(LocalDate start, LocalDate end, Aggregation agg) {
        // Use original payments table for accurate payment amounts (not merged orders)
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusNanos(1);
        List<PaymentEntity> payments = paymentRepository.findByPaymentDateTimeBetween(startDateTime, endDateTime);
        
        log.debug("paymentsByTime: Found {} payments between {} and {} from payments table", payments.size(), start, end);
        
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        Map<LocalDate, Integer> counts = new TreeMap<>();
        
        payments.forEach(p -> {
            LocalDate key = aggregateDate(p.getPaymentDateTime().toLocalDate(), agg);
            BigDecimal amount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            totals.merge(key, amount, BigDecimal::add);
            counts.merge(key, 1, Integer::sum);
            
            // Debug logging for specific date if requested
            if (start.equals(end) && p.getPaymentDateTime() != null && p.getPaymentDateTime().toLocalDate().equals(start)) {
                log.debug("Payment for {}: OrderId={}, SKU={}, Amount={}", 
                    p.getPaymentDateTime().toLocalDate(), p.getOrderId(), p.getSku(), amount);
            }
        });
        
        // Log totals for debugging
        totals.forEach((date, total) -> {
            log.debug("Date {}: {} payments, total amount: {}", date, counts.get(date), total);
        });
        
        List<TimeSeriesPoint> points = totals.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }

    public List<Map<String, Object>> topOrderedSkus(LocalDate start, LocalDate end, int limit) {
        try {
            var rows = mergedOrderRepository.findTopOrderedSkus(start, end);
            
            if (rows.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = rows.stream().limit(limit).map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sku", (String) r[0]);
                
                // Handle different numeric types that sum() can return
                Number quantity;
                if (r[1] instanceof BigInteger) {
                    quantity = ((BigInteger) r[1]).longValue();
                } else if (r[1] instanceof BigDecimal) {
                    quantity = ((BigDecimal) r[1]).longValue();
                } else if (r[1] instanceof Long) {
                    quantity = (Long) r[1];
                } else if (r[1] instanceof Integer) {
                    quantity = ((Integer) r[1]).longValue();
                } else {
                    // Fallback: try to parse as string
                    try {
                        quantity = Long.parseLong(r[1].toString());
                    } catch (NumberFormatException e) {
                        quantity = 0L;
                    }
                }
                
                m.put("quantity", quantity);
                return m;
            }).toList();
            
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> topProfitableSkus(LocalDate start, LocalDate end, int limit) {
        try {
            var rows = mergedOrderRepository.findTopProfitableSkus(start, end);
            
            if (rows.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = rows.stream().limit(limit).map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sku", (String) r[0]);
                
                // Handle different numeric types that sum() can return
                Number profit;
                if (r[1] instanceof BigInteger) {
                    profit = ((BigInteger) r[1]).longValue();
                } else if (r[1] instanceof BigDecimal) {
                    profit = ((BigDecimal) r[1]).longValue();
                } else if (r[1] instanceof Long) {
                    profit = (Long) r[1];
                } else if (r[1] instanceof Integer) {
                    profit = ((Integer) r[1]).longValue();
                } else {
                    // Fallback: try to parse as string
                    try {
                        profit = Long.parseLong(r[1].toString());
                    } catch (NumberFormatException e) {
                        profit = 0L;
                    }
                }
                
                m.put("profit", profit);
                return m;
            }).toList();
            
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get purchase price for a SKU (from group or individual price)
     * This method integrates with SKU groups while maintaining backward compatibility
     */
    private BigDecimal getPurchasePriceForSku(String sku) {
        try {
            // First try to get price from SKU group mapping
            var skuGroupService = applicationContext.getBean(SkuGroupService.class);
            BigDecimal price = skuGroupService.getPurchasePriceForSku(sku);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return price;
            }
            // If zero/absent, fallback to individual SKU price
            return skuPriceRepository.findBySku(sku)
                    .map(SkuPriceEntity::getPurchasePrice)
                    .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            // Fallback to individual SKU price (existing behavior)
            return skuPriceRepository.findBySku(sku)
                    .map(SkuPriceEntity::getPurchasePrice)
                    .orElseGet(() -> BigDecimal.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextDouble(10.0, 80.0)).setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * Compute purchase price for a specific order using multiple fallbacks.
     */
    private BigDecimal computePurchasePrice(OrderEntity order) {
        if (order == null) return BigDecimal.ZERO;
        BigDecimal fromCatalog = getPurchasePriceForSku(order.getSku());
        if (fromCatalog != null && fromCatalog.compareTo(BigDecimal.ZERO) > 0) return fromCatalog;
        if (order.getSupplierDiscountedPrice() != null && order.getSupplierDiscountedPrice().compareTo(BigDecimal.ZERO) > 0) {
            return order.getSupplierDiscountedPrice();
        }
        if (order.getSupplierListedPrice() != null && order.getSupplierListedPrice().compareTo(BigDecimal.ZERO) > 0) {
            return order.getSupplierListedPrice();
        }
        return BigDecimal.ZERO;
    }

    public List<Map<String, Object>> getOrderCountsByStatus(LocalDate start, LocalDate end) {
        try {
            var rows = mergedOrderRepository.findOrderStatusCounts(start, end);
            
            if (rows.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = rows.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("status", (String) r[0]);
                m.put("count", ((Number) r[1]).longValue());
                return m;
            }).sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
            .toList();

            return result;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
    
    // New method to get order counts by status with month breakdown
    public List<Map<String, Object>> getOrderCountsByStatusWithMonthBreakdown() {
        try {
            log.debug("getOrderCountsByStatusWithMonthBreakdown called");
            
            // Use merged orders table for consistency with getOrderCountsByStatus
            // This ensures both endpoints return the same data
            var rows = mergedOrderRepository.findOrderStatusCountsWithMonth();
            log.debug("Repository returned {} rows from merged orders table", rows.size());
            
            if (rows.isEmpty()) {
                log.debug("No rows returned from repository");
                return new ArrayList<>();
            }
            
            // Convert to result format with month information
            List<Map<String, Object>> result = rows.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("status", (String) r[0]);
                m.put("count", ((Number) r[1]).longValue());
                m.put("month", ((Number) r[2]).intValue());
                m.put("year", ((Number) r[3]).intValue());
                
                // Add month name for better display
                String monthName = getMonthName(((Number) r[2]).intValue());
                m.put("monthName", monthName);
                
                return m;
            }).toList();
            
            log.debug("Returning {} items with month breakdown", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Error in getOrderCountsByStatusWithMonthBreakdown: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String getMonthName(int month) {
        String[] monthNames = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return monthNames[month - 1]; // month is 1-based
    }

    public ChartResponse<TimeSeriesPoint> profitTrend(LocalDate start, LocalDate end, Aggregation agg) {
        var mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        
        mergedOrders.forEach(merged -> {
            BigDecimal settlementAmount = merged.getSettlementAmount() != null ? merged.getSettlementAmount() : BigDecimal.ZERO;
            
            if (settlementAmount.compareTo(BigDecimal.ZERO) > 0 && merged.getSkuId() != null && merged.getQuantity() != null) {
                try {
                    // Get purchase price for this SKU
                    BigDecimal purchasePrice = getPurchasePriceForSku(merged.getSkuId());
                    
                    if (purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate COGS: Purchase Price × Quantity
                        BigDecimal cogs = purchasePrice.multiply(BigDecimal.valueOf(merged.getQuantity()));
                        
                        // Calculate profit: Revenue - COGS
                        BigDecimal profit = settlementAmount.subtract(cogs);
                        
                        if (profit.signum() > 0) {
                            LocalDate key = aggregateDate(merged.getOrderDate(), agg);
                            totals.merge(key, profit, BigDecimal::add);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error calculating profit for SKU {}: {}", merged.getSkuId(), e.getMessage());
                }
            }
        });
        
        List<TimeSeriesPoint> points = totals.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }

    public ChartResponse<TimeSeriesPoint> lossTrend(LocalDate start, LocalDate end, Aggregation agg) {
        var mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        
        mergedOrders.forEach(merged -> {
            BigDecimal settlementAmount = merged.getSettlementAmount() != null ? merged.getSettlementAmount() : BigDecimal.ZERO;
            
            if (settlementAmount.compareTo(BigDecimal.ZERO) > 0 && merged.getSkuId() != null && merged.getQuantity() != null) {
                try {
                    // Get purchase price for this SKU
                    BigDecimal purchasePrice = getPurchasePriceForSku(merged.getSkuId());
                    
                    if (purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate COGS: Purchase Price × Quantity
                        BigDecimal cogs = purchasePrice.multiply(BigDecimal.valueOf(merged.getQuantity()));
                        
                        // Calculate profit/loss: Revenue - COGS
                        BigDecimal netProfit = settlementAmount.subtract(cogs);
                        
                        if (netProfit.signum() < 0) {
                            LocalDate key = aggregateDate(merged.getOrderDate(), agg);
                            totals.merge(key, netProfit.abs(), BigDecimal::add);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error calculating loss for SKU {}: {}", merged.getSkuId(), e.getMessage());
                }
            }
        });
        
        List<TimeSeriesPoint> points = totals.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }
    
    /**
     * Get loss orders for a specific date range
     * Returns orders that resulted in losses despite being delivered and paid
     */
    public Map<String, Object> getLossOrders(LocalDate start, LocalDate end) {
        try {
            var mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
            List<Map<String, Object>> lossOrders = new ArrayList<>();
            
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalCogs = BigDecimal.ZERO;
            BigDecimal totalLoss = BigDecimal.ZERO;
            long totalQuantity = 0;
            
            for (MergedOrderPaymentEntity merged : mergedOrders) {
                if (merged.getSettlementAmount() == null || merged.getSettlementAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                    merged.getSkuId() == null || merged.getQuantity() == null ||
                    !"DELIVERED".equals(merged.getOrderStatus())) {
                    continue; // Skip invalid or non-delivered orders
                }
                
                try {
                    // Get purchase price for this SKU
                    BigDecimal purchasePrice = getPurchasePriceForSku(merged.getSkuId());
                    
                    if (purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate COGS: Purchase Price × Quantity
                        BigDecimal cogs = purchasePrice.multiply(BigDecimal.valueOf(merged.getQuantity()));
                        
                        // Calculate profit/loss: Revenue - COGS
                        BigDecimal netProfit = merged.getSettlementAmount().subtract(cogs);
                        
                        // Only include orders that resulted in losses
                        if (netProfit.signum() < 0) {
                            Map<String, Object> orderData = new LinkedHashMap<>();
                            orderData.put("orderId", merged.getOrderId());
                            orderData.put("skuId", merged.getSkuId());
                            orderData.put("quantity", merged.getQuantity());
                            orderData.put("settlementAmount", merged.getSettlementAmount());
                            orderData.put("purchasePrice", purchasePrice);
                            orderData.put("cogs", cogs);
                            orderData.put("lossAmount", netProfit.abs());
                            orderData.put("orderStatus", merged.getOrderStatus());
                            orderData.put("orderDate", merged.getOrderDate());
                            
                            lossOrders.add(orderData);
                            
                            // Update totals
                            totalRevenue = totalRevenue.add(merged.getSettlementAmount());
                            totalCogs = totalCogs.add(cogs);
                            totalLoss = totalLoss.add(netProfit.abs());
                            totalQuantity += merged.getQuantity();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error calculating loss for SKU {}: {}", merged.getSkuId(), e.getMessage());
                }
            }
            
            // Sort by loss amount (highest first)
            lossOrders.sort((a, b) -> {
                BigDecimal lossA = (BigDecimal) a.get("lossAmount");
                BigDecimal lossB = (BigDecimal) b.get("lossAmount");
                return lossB.compareTo(lossA);
            });
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orders", lossOrders);
            result.put("summary", Map.of(
                "totalOrders", lossOrders.size(),
                "totalQuantity", totalQuantity,
                "totalRevenue", totalRevenue,
                "totalCogs", totalCogs,
                "totalLoss", totalLoss
            ));
            
            return result;
        } catch (Exception e) {
            log.error("Error getting loss orders: {}", e.getMessage(), e);
            return Map.of(
                "orders", new ArrayList<>(),
                "summary", Map.of(
                    "totalOrders", 0,
                    "totalQuantity", 0L,
                    "totalRevenue", BigDecimal.ZERO,
                    "totalCogs", BigDecimal.ZERO,
                    "totalLoss", BigDecimal.ZERO
                )
            );
        }
    }

    public LocalDate aggregateDate(LocalDate date, Aggregation agg) {
        return switch (agg) {
            case DAY -> date;
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.with(TemporalAdjusters.firstDayOfYear());
            case QUARTER -> firstDayOfQuarter(date);
        };
    }

    public LocalDate firstDayOfQuarter(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3;
        int firstMonth = quarter * 3 + 1;
        return LocalDate.of(date.getYear(), firstMonth, 1);
    }

    /**
     * Monthly summary metrics for a given year/month
     */
        public Map<String, Object> getMonthlySummary(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());

        // Get data from merged_orders table
        List<MergedOrderPaymentEntity> mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        long totalOrders = mergedOrders.size();

        // Calculate revenue and profit/loss from merged data
        for (MergedOrderPaymentEntity merged : mergedOrders) {
            BigDecimal settlementAmount = merged.getSettlementAmount() != null ? merged.getSettlementAmount() : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(settlementAmount);
            
            // Calculate actual profit/loss: Revenue - Cost of Goods Sold
            BigDecimal profit = BigDecimal.ZERO;
            BigDecimal loss = BigDecimal.ZERO;
            
            if (merged.getSkuId() != null && merged.getQuantity() != null && settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Get purchase price for this SKU
                    BigDecimal purchasePrice = getPurchasePriceForSku(merged.getSkuId());
                    
                    if (purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate COGS: Purchase Price × Quantity
                        BigDecimal cogs = purchasePrice.multiply(BigDecimal.valueOf(merged.getQuantity()));
                        
                        // Calculate profit/loss: Revenue - COGS
                        BigDecimal netProfit = settlementAmount.subtract(cogs);
                        
                        if (netProfit.signum() > 0) {
                            totalProfit = totalProfit.add(netProfit);
                        } else if (netProfit.signum() < 0) {
                            totalLoss = totalLoss.add(netProfit.abs());
                        }
                    } else {
                        log.debug("SKU {} has no purchase price, skipping profit calculation", merged.getSkuId());
                    }
                } catch (Exception e) {
                    log.warn("Error calculating profit for SKU {}: {}", merged.getSkuId(), e.getMessage());
                }
            }
        }

        BigDecimal netIncome = totalProfit.subtract(totalLoss);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("year", year);
        summary.put("month", month);
        summary.put("start", start);
        summary.put("end", end);
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalProfit", totalProfit);
        summary.put("totalOrders", totalOrders);
        summary.put("totalLoss", totalLoss);
        summary.put("netIncome", netIncome);
        summary.put("paymentsReceived", totalRevenue);
        return summary;
    }



    /**
     * Diagnostics: show how payments join to orders and computed costs for a given month
     */
    public Map<String, Object> getMonthlyDiagnostics(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);

        var payments = paymentRepository.findByPaymentDateTimeBetween(s, e);
        Map<String, BigDecimal> revenueByOrderId = payments.stream()
                .filter(p -> p.getOrderId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getOrderId().trim(),
                        Collectors.mapping(
                                p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        List<String> orderIds = new ArrayList<>(revenueByOrderId.keySet());
        Map<String, OrderEntity> ordersById = new HashMap<>();
        if (!orderIds.isEmpty()) {
            var found = orderRepository.findByOrderIdIn(orderIds);
            for (OrderEntity o : found) {
                if (o.getOrderId() != null) ordersById.put(o.getOrderId().trim(), o);
            }
        }

        int matched = 0;
        int zeroCost = 0;
        List<Map<String, Object>> samples = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> en : revenueByOrderId.entrySet()) {
            String orderId = en.getKey();
            OrderEntity o = ordersById.get(orderId);
            boolean hasOrder = o != null;
            if (hasOrder) matched++;
            BigDecimal qty = hasOrder && o.getQuantity() != null ? BigDecimal.valueOf(o.getQuantity()) : BigDecimal.ZERO;
            BigDecimal purchase = hasOrder ? computePurchasePrice(o) : BigDecimal.ZERO;
            BigDecimal cost = purchase.multiply(qty);
            if (cost.compareTo(BigDecimal.ZERO) == 0) zeroCost++;
            if (samples.size() < 10) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("orderId", orderId);
                row.put("revenue", en.getValue());
                row.put("hasOrder", hasOrder);
                if (hasOrder) {
                    row.put("sku", o.getSku());
                    row.put("quantity", o.getQuantity());
                    row.put("purchasePriceUsed", purchase);
                    row.put("cost", cost);
                    row.put("profit", en.getValue().subtract(cost));
                }
                samples.add(row);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("month", month);
        out.put("paymentsCount", payments.size());
        out.put("paymentOrderIds", revenueByOrderId.size());
        out.put("matchedOrders", matched);
        out.put("zeroCostOrders", zeroCost);
        out.put("samples", samples);
        return out;
    }
}


