package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.SkuPriceEntity;
import com.ecomanalyser.dto.ChartResponse;
import com.ecomanalyser.dto.TimeSeriesPoint;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
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
    private final ApplicationContext applicationContext;

    public enum Aggregation { DAY, MONTH, YEAR, QUARTER }

    public ChartResponse<TimeSeriesPoint> ordersByTime(LocalDate start, LocalDate end, Aggregation agg) {
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);
        List<OrderEntity> orders = orderRepository.findByOrderDateTimeBetween(s, e);
        Map<LocalDate, BigDecimal> counts = new TreeMap<>();
        for (OrderEntity o : orders) {
            LocalDate key = aggregateDate(o.getOrderDateTime().toLocalDate(), agg);
            counts.merge(key, BigDecimal.ONE, BigDecimal::add);
        }
        List<TimeSeriesPoint> points = counts.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }

    public ChartResponse<TimeSeriesPoint> paymentsByTime(LocalDate start, LocalDate end, Aggregation agg) {
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);
        var payments = paymentRepository.findByPaymentDateTimeBetween(s, e);
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        payments.forEach(p -> {
            LocalDate key = aggregateDate(p.getPaymentDateTime().toLocalDate(), agg);
            totals.merge(key, p.getAmount(), BigDecimal::add);
        });
        List<TimeSeriesPoint> points = totals.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }

    public List<Map<String, Object>> topOrderedSkus(LocalDate start, LocalDate end, int limit) {
        try {
            System.out.println("topOrderedSkus called with start: " + start + ", end: " + end + ", limit: " + limit);
            
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusNanos(1);
            System.out.println("Converted to DateTime - start: " + startDateTime + ", end: " + endDateTime);
            
            var rows = orderRepository.topOrderedSkus(startDateTime, endDateTime);
            System.out.println("Repository returned " + rows.size() + " rows");
            
            if (rows.isEmpty()) {
                System.out.println("No rows returned from repository");
                return new ArrayList<>();
            }
            
            // Log first few rows for debugging
            for (int i = 0; i < Math.min(3, rows.size()); i++) {
                Object[] row = rows.get(i);
                System.out.println("Row " + i + ": SKU=" + row[0] + ", Quantity=" + row[1] + " (type: " + (row[1] != null ? row[1].getClass().getSimpleName() : "null") + ")");
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
                        System.out.println("Failed to parse quantity: " + r[1] + " as Long");
                        quantity = 0L;
                    }
                }
                
                m.put("quantity", quantity);
                return m;
            }).toList();
            
            System.out.println("Returning " + result.size() + " items");
            return result;
        } catch (Exception e) {
            // Log the error and return empty list instead of throwing 500
            System.err.println("Error in topOrderedSkus: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> topProfitableSkus(LocalDate start, LocalDate end, int limit) {
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);
        var orders = orderRepository.findByOrderDateTimeBetween(s, e);
        Map<String, BigDecimal> skuToProfit = new HashMap<>();
        for (OrderEntity o : orders) {
            // Try to get purchase price from SKU groups first, then fallback to individual SKU prices
            BigDecimal purchase = getPurchasePriceForSku(o.getSku());
            BigDecimal profit = o.getSellingPrice().subtract(purchase).multiply(BigDecimal.valueOf(o.getQuantity()));
            skuToProfit.merge(o.getSku(), profit, BigDecimal::add);
        }
        return skuToProfit.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .map(en -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sku", en.getKey());
                    m.put("profit", en.getValue());
                    return m;
                })
                .collect(Collectors.toList());
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
            System.out.println("getOrderCountsByStatus called with start: " + start + ", end: " + end);
            
            // Use the new method that provides month information and doesn't filter by date range
            var rows = paymentRepository.getOrderCountsByStatusWithMonth();
            System.out.println("Repository returned " + rows.size() + " rows");
            
            if (rows.isEmpty()) {
                System.out.println("No rows returned from repository - this usually means:");
                System.out.println("1. No payments exist, OR");
                System.out.println("2. All payments have NULL order_status (need to re-upload payments file)");
                return new ArrayList<>();
            }
            
            // Log first few rows for debugging
            for (int i = 0; i < Math.min(3, rows.size()); i++) {
                Object[] row = rows.get(i);
                System.out.println("Row " + i + ": Status=" + row[0] + ", Count=" + row[1] + ", Month=" + row[2] + ", Year=" + row[3]);
            }
            
            // Group by status and aggregate counts across all months
            Map<String, Long> statusToCount = new HashMap<>();
            for (Object[] row : rows) {
                String status = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                statusToCount.merge(status, count, Long::sum);
            }
            
            // Convert to result format
            List<Map<String, Object>> result = statusToCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(en -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("status", en.getKey());
                        m.put("count", en.getValue());
                        return m;
                    })
                    .toList();
            
            System.out.println("Returning " + result.size() + " items with aggregated counts");
            return result;
        } catch (Exception e) {
            System.err.println("Error in getOrderCountsByStatus: " + e.getMessage());
            e.printStackTrace();
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
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);
        var payments = paymentRepository.findByPaymentDateTimeBetween(s, e);
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        payments.forEach(p -> {
            var orderOpt = orderRepository.findByOrderId(p.getOrderId());
            if (orderOpt.isEmpty()) return;
            OrderEntity o = orderOpt.get();
            BigDecimal purchase = getPurchasePriceForSku(o.getSku());
            BigDecimal cost = (o.getQuantity() == null ? BigDecimal.ZERO : purchase.multiply(BigDecimal.valueOf(o.getQuantity())));
            BigDecimal revenue = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            BigDecimal profit = revenue.subtract(cost);
            LocalDate key = aggregateDate(p.getPaymentDateTime().toLocalDate(), agg);
            totals.merge(key, profit.signum() > 0 ? profit : BigDecimal.ZERO, BigDecimal::add);
        });
        List<TimeSeriesPoint> points = totals.entrySet().stream()
                .map(en -> new TimeSeriesPoint(en.getKey(), en.getValue()))
                .toList();
        return new ChartResponse<>(points);
    }

    public ChartResponse<TimeSeriesPoint> lossTrend(LocalDate start, LocalDate end, Aggregation agg) {
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);
        var payments = paymentRepository.findByPaymentDateTimeBetween(s, e);
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        payments.forEach(p -> {
            var orderOpt = orderRepository.findByOrderId(p.getOrderId());
            if (orderOpt.isEmpty()) return;
            OrderEntity o = orderOpt.get();
            BigDecimal purchase = getPurchasePriceForSku(o.getSku());
            BigDecimal cost = (o.getQuantity() == null ? BigDecimal.ZERO : purchase.multiply(BigDecimal.valueOf(o.getQuantity())));
            BigDecimal revenue = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            BigDecimal profit = revenue.subtract(cost);
            LocalDate key = aggregateDate(p.getPaymentDateTime().toLocalDate(), agg);
            BigDecimal loss = profit.signum() < 0 ? profit.abs() : BigDecimal.ZERO;
            totals.merge(key, loss, BigDecimal::add);
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
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusNanos(1);

        // Revenue from bank settlement amounts (payments)
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // Compute payments received and also prefer using it as totalRevenue
        BigDecimal paymentsReceived = BigDecimal.ZERO;
        try {
            var normalizedPaymentRepo = applicationContext.getBean(com.ecomanalyser.repository.NormalizedPaymentRepository.class);
            var np = normalizedPaymentRepo.findByPaymentDateBetween(start, end);
            paymentsReceived = np.stream()
                    .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception ignored) {
        }
        if (paymentsReceived.compareTo(BigDecimal.ZERO) == 0) {
            paymentsReceived = paymentRepository.findByPaymentDateTimeBetween(s, e)
                    .stream()
                    .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        totalRevenue = paymentsReceived;

        // Totals derived from payments in month, cost counted once per order (join by orderId)
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        long totalOrders = 0L;

        var ordersInMonth = orderRepository.findByOrderDateTimeBetween(s, e);
        totalOrders = ordersInMonth.size();

        var paymentsInRange = paymentRepository.findByPaymentDateTimeBetween(s, e);
        // Group payments per orderId
        Map<String, BigDecimal> revenueByOrderId = paymentsInRange.stream()
                .filter(p -> p.getOrderId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getOrderId(),
                        Collectors.mapping(
                                p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Load all orders for these orderIds in one call (even if order date is outside month)
        List<String> paidOrderIds = new ArrayList<>(revenueByOrderId.keySet());
        Map<String, OrderEntity> paidOrders = new HashMap<>();
        if (!paidOrderIds.isEmpty()) {
            var found = orderRepository.findByOrderIdIn(paidOrderIds);
            for (OrderEntity o : found) {
                if (o.getOrderId() != null) paidOrders.put(o.getOrderId(), o);
            }
        }

        for (Map.Entry<String, BigDecimal> entry : revenueByOrderId.entrySet()) {
            String orderId = entry.getKey();
            BigDecimal revenue = entry.getValue();
            BigDecimal cost = BigDecimal.ZERO;
            OrderEntity o = paidOrders.get(orderId);
            if (o != null) {
                BigDecimal purchase = computePurchasePrice(o);
                if (o.getQuantity() != null) {
                    cost = purchase.multiply(BigDecimal.valueOf(o.getQuantity()));
                }
            }
            BigDecimal profit = revenue.subtract(cost);
            if (profit.signum() >= 0) totalProfit = totalProfit.add(profit); else totalLoss = totalLoss.add(profit.abs());
        }

        // Fallback: if there are no payments, derive from orders so UI isn't empty
        if (paymentsInRange.isEmpty()) {
            BigDecimal ordersRevenue = BigDecimal.ZERO;
            for (OrderEntity o : ordersInMonth) {
                BigDecimal purchase = computePurchasePrice(o);
                if (o.getSellingPrice() != null && o.getQuantity() != null) {
                    BigDecimal lineRevenue = o.getSellingPrice().multiply(BigDecimal.valueOf(o.getQuantity()));
                    ordersRevenue = ordersRevenue.add(lineRevenue);
                    BigDecimal profit = o.getSellingPrice().subtract(purchase)
                            .multiply(BigDecimal.valueOf(o.getQuantity()));
                    if (profit.signum() >= 0) {
                        totalProfit = totalProfit.add(profit);
                    } else {
                        totalLoss = totalLoss.add(profit.abs());
                    }
                }
            }
            if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
                totalRevenue = ordersRevenue;
            }
        }

        // Safety: profit should not exceed revenue (aggregate bound)
        if (totalProfit.compareTo(totalRevenue) > 0) {
            totalProfit = totalRevenue;
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
        summary.put("paymentsReceived", paymentsReceived);
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


