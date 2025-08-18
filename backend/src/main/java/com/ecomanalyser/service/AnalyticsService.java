package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
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
        List<MergedOrderPaymentEntity> payments = mergedOrderRepository.findByPaymentDateBetween(start, end);
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        payments.forEach(p -> {
            LocalDate key = aggregateDate(p.getPaymentDate(), agg);
            totals.merge(key, p.getSettlementAmount() != null ? p.getSettlementAmount() : BigDecimal.ZERO, BigDecimal::add);
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
            System.out.println("getOrderCountsByStatusWithMonthBreakdown called");
            
            var rows = paymentRepository.getOrderCountsByStatusWithMonth();
            System.out.println("Repository returned " + rows.size() + " rows");
            
            if (rows.isEmpty()) {
                System.out.println("No rows returned from repository");
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
            
            System.out.println("Returning " + result.size() + " items with month breakdown");
            return result;
        } catch (Exception e) {
            System.err.println("Error in getOrderCountsByStatusWithMonthBreakdown: " + e.getMessage());
            e.printStackTrace();
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
            BigDecimal orderAmount = merged.getOrderAmount() != null ? merged.getOrderAmount() : BigDecimal.ZERO;
            BigDecimal profit = settlementAmount.subtract(orderAmount);
            
            if (profit.signum() > 0) {
                LocalDate key = aggregateDate(merged.getOrderDate(), agg);
                totals.merge(key, profit, BigDecimal::add);
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
            BigDecimal orderAmount = merged.getOrderAmount() != null ? merged.getOrderAmount() : BigDecimal.ZERO;
            BigDecimal profit = settlementAmount.subtract(orderAmount);
            
            if (profit.signum() < 0) {
                LocalDate key = aggregateDate(merged.getOrderDate(), agg);
                totals.merge(key, profit.abs(), BigDecimal::add);
            }
        });
        
        List<TimeSeriesPoint> points = totals.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
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
            
            // Calculate profit/loss based on settlement amount vs order amount
            BigDecimal orderAmount = merged.getOrderAmount() != null ? merged.getOrderAmount() : BigDecimal.ZERO;
            if (settlementAmount.compareTo(orderAmount) > 0) {
                totalProfit = totalProfit.add(settlementAmount.subtract(orderAmount));
            } else if (settlementAmount.compareTo(orderAmount) < 0) {
                totalLoss = totalLoss.add(orderAmount.subtract(settlementAmount));
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


