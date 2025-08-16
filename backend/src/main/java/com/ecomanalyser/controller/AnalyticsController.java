package com.ecomanalyser.controller;

import com.ecomanalyser.dto.ChartResponse;
import com.ecomanalyser.dto.TimeSeriesPoint;
import com.ecomanalyser.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.domain.OrderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;
    private final OrderRepository orderRepository;

    @GetMapping("/orders-by-time")
    public ChartResponse<TimeSeriesPoint> ordersByTime(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam("agg") AnalyticsService.Aggregation agg
    ) {
        return analyticsService.ordersByTime(start, end, agg);
    }

    @GetMapping("/payments-by-time")
    public ChartResponse<TimeSeriesPoint> paymentsByTime(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam("agg") AnalyticsService.Aggregation agg
    ) {
        return analyticsService.paymentsByTime(start, end, agg);
    }

    @GetMapping("/top-ordered")
    public List<Map<String, Object>> topOrdered(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return analyticsService.topOrderedSkus(start, end, limit);
    }

    @GetMapping("/top-profitable")
    public List<Map<String, Object>> topProfitableSkus(
            @RequestParam("start") LocalDate start,
            @RequestParam("end") LocalDate end,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return analyticsService.topProfitableSkus(start, end, limit);
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<List<Map<String, Object>>> getOrderCountsByStatus(
            @RequestParam("start") LocalDate start,
            @RequestParam("end") LocalDate end) {
        try {
            var result = analyticsService.getOrderCountsByStatus(start, end);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting order counts by status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/orders-by-status-monthly")
    public ResponseEntity<List<Map<String, Object>>> getOrderCountsByStatusWithMonthBreakdown() {
        try {
            var result = analyticsService.getOrderCountsByStatusWithMonthBreakdown();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting order counts by status with month breakdown: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/profit-trend")
    public ChartResponse<TimeSeriesPoint> profitTrend(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam("agg") AnalyticsService.Aggregation agg
    ) {
        return analyticsService.profitTrend(start, end, agg);
    }

    @GetMapping("/loss-trend")
    public ChartResponse<TimeSeriesPoint> lossTrend(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam("agg") AnalyticsService.Aggregation agg
    ) {
        return analyticsService.lossTrend(start, end, agg);
    }

    @GetMapping("/debug/orders")
    public Map<String, Object> debugOrders() {
        Map<String, Object> debug = new HashMap<>();
        try {
            var allOrders = orderRepository.findAll();
            debug.put("totalOrders", allOrders.size());
            
            if (!allOrders.isEmpty()) {
                var firstOrder = allOrders.stream().min(Comparator.comparing(OrderEntity::getOrderDateTime)).orElse(null);
                var lastOrder = allOrders.stream().max(Comparator.comparing(OrderEntity::getOrderDateTime)).orElse(null);
                
                if (firstOrder != null) {
                    debug.put("firstOrderDate", firstOrder.getOrderDateTime());
                    debug.put("firstOrderSku", firstOrder.getSku());
                }
                if (lastOrder != null) {
                    debug.put("lastOrderDate", lastOrder.getOrderDateTime());
                    debug.put("lastOrderSku", lastOrder.getSku());
                }
            }
            
            debug.put("success", true);
        } catch (Exception e) {
            debug.put("success", false);
            debug.put("error", e.getMessage());
        }
        return debug;
    }
}


