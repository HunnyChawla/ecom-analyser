package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.SkuPriceEntity;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SkuPriceRepository skuPriceRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2025, 8, 1);
        endDate = LocalDate.of(2025, 8, 15);
        startDateTime = startDate.atStartOfDay();
        endDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);
    }

    @Test
    void testTopOrderedSkus() {
        // Given
        Object[] row1 = {"SKU1", 10L};
        Object[] row2 = {"SKU2", 5L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(orderRepository.topOrderedSkus(startDateTime, endDateTime))
                .thenReturn(mockData);

        // When
        List<Map<String, Object>> result = analyticsService.topOrderedSkus(startDate, endDate, 5);

        // Then
        assertEquals(2, result.size());
        assertEquals("SKU1", result.get(0).get("sku"));
        assertEquals(10L, result.get(0).get("quantity"));
        assertEquals("SKU2", result.get(1).get("sku"));
        assertEquals(5L, result.get(1).get("quantity"));
    }

    @Test
    void testTopOrderedSkusWithEmptyData() {
        // Given
        when(orderRepository.topOrderedSkus(startDateTime, endDateTime))
                .thenReturn(List.of());

        // When
        List<Map<String, Object>> result = analyticsService.topOrderedSkus(startDate, endDate, 5);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testTopProfitableSkus() {
        // Given
        OrderEntity order1 = OrderEntity.builder()
                .sku("SKU1")
                .sellingPrice(new BigDecimal("100"))
                .quantity(2)
                .build();
        OrderEntity order2 = OrderEntity.builder()
                .sku("SKU2")
                .sellingPrice(new BigDecimal("50"))
                .quantity(1)
                .build();

        List<OrderEntity> orders = Arrays.asList(order1, order2);

        when(orderRepository.findByOrderDateTimeBetween(startDateTime, endDateTime))
                .thenReturn(orders);
        when(skuPriceRepository.findBySku("SKU1"))
                .thenReturn(Optional.of(SkuPriceEntity.builder()
                        .purchasePrice(new BigDecimal("60"))
                        .build()));
        when(skuPriceRepository.findBySku("SKU2"))
                .thenReturn(Optional.of(SkuPriceEntity.builder()
                        .purchasePrice(new BigDecimal("30"))
                        .build()));

        // When
        List<Map<String, Object>> result = analyticsService.topProfitableSkus(startDate, endDate, 5);

        // Then
        assertEquals(2, result.size());
        assertEquals("SKU1", result.get(0).get("sku"));
        assertEquals(new BigDecimal("80"), result.get(0).get("profit")); // (100-60)*2
        assertEquals("SKU2", result.get(1).get("sku"));
        assertEquals(new BigDecimal("20"), result.get(1).get("profit")); // (50-30)*1
    }

    @Test
    void testTopProfitableSkusWithRandomPurchasePrice() {
        // Given
        OrderEntity order = OrderEntity.builder()
                .sku("SKU1")
                .sellingPrice(new BigDecimal("100"))
                .quantity(1)
                .build();

        when(orderRepository.findByOrderDateTimeBetween(startDateTime, endDateTime))
                .thenReturn(List.of(order));
        when(skuPriceRepository.findBySku("SKU1"))
                .thenReturn(Optional.empty());

        // When
        List<Map<String, Object>> result = analyticsService.topProfitableSkus(startDate, endDate, 5);

        // Then
        assertEquals(1, result.size());
        assertNotNull(result.get(0).get("profit"));
        // Random price should be between 10-80, so profit should be positive
        assertTrue(((BigDecimal) result.get(0).get("profit")).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGetOrderCountsByStatus() {
        // Given
        Object[] row1 = {"DELIVERED", 100L};
        Object[] row2 = {"RTO", 50L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(paymentRepository.getOrderCountsByStatus(startDateTime, endDateTime))
                .thenReturn(mockData);

        // When
        List<Map<String, Object>> result = analyticsService.getOrderCountsByStatus(startDate, endDate);

        // Then
        assertEquals(2, result.size());
        assertEquals("DELIVERED", result.get(0).get("status"));
        assertEquals(100L, result.get(0).get("count"));
        assertEquals("RTO", result.get(1).get("status"));
        assertEquals(50L, result.get(1).get("count"));
    }

    @Test
    void testGetOrderCountsByStatusWithEmptyData() {
        // Given
        when(paymentRepository.getOrderCountsByStatus(startDateTime, endDateTime))
                .thenReturn(List.of());

        // When
        List<Map<String, Object>> result = analyticsService.getOrderCountsByStatus(startDate, endDate);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testOrdersByTime() {
        // Given
        OrderEntity order1 = OrderEntity.builder()
                .orderDateTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .quantity(2)
                .build();
        OrderEntity order2 = OrderEntity.builder()
                .orderDateTime(LocalDateTime.of(2025, 8, 1, 14, 0))
                .quantity(3)
                .build();

        when(orderRepository.findByOrderDateTimeBetween(startDateTime, endDateTime))
                .thenReturn(Arrays.asList(order1, order2));

        // When
        var result = analyticsService.ordersByTime(startDate, endDate, AnalyticsService.Aggregation.DAY);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(5, result.getData().get(0).getValue().intValue());
    }

    @Test
    void testPaymentsByTime() {
        // Given
        PaymentEntity payment1 = PaymentEntity.builder()
                .paymentDateTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .amount(new BigDecimal("100"))
                .build();
        PaymentEntity payment2 = PaymentEntity.builder()
                .paymentDateTime(LocalDateTime.of(2025, 8, 1, 14, 0))
                .amount(new BigDecimal("200"))
                .build();

        when(paymentRepository.findByPaymentDateTimeBetween(startDateTime, endDateTime))
                .thenReturn(Arrays.asList(payment1, payment2));

        // When
        var result = analyticsService.paymentsByTime(startDate, endDate, AnalyticsService.Aggregation.DAY);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(new BigDecimal("300"), result.getData().get(0).getValue());
    }

    @Test
    void testProfitTrend() {
        // Given
        OrderEntity order = OrderEntity.builder()
                .orderDateTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .sellingPrice(new BigDecimal("100"))
                .quantity(1)
                .sku("SKU1")
                .build();

        when(orderRepository.findByOrderDateTimeBetween(startDateTime, endDateTime))
                .thenReturn(List.of(order));
        when(skuPriceRepository.findBySku("SKU1"))
                .thenReturn(Optional.of(SkuPriceEntity.builder()
                        .purchasePrice(new BigDecimal("60"))
                        .build()));

        // When
        var result = analyticsService.profitTrend(startDate, endDate, AnalyticsService.Aggregation.DAY);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(new BigDecimal("40"), result.getData().get(0).getValue());
    }

    @Test
    void testLossTrend() {
        // Given
        OrderEntity order = OrderEntity.builder()
                .orderDateTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .sellingPrice(new BigDecimal("50"))
                .quantity(1)
                .sku("SKU1")
                .build();

        when(orderRepository.findByOrderDateTimeBetween(startDateTime, endDateTime))
                .thenReturn(List.of(order));
        when(skuPriceRepository.findBySku("SKU1"))
                .thenReturn(Optional.of(SkuPriceEntity.builder()
                        .purchasePrice(new BigDecimal("100"))
                        .build()));

        // When
        var result = analyticsService.lossTrend(startDate, endDate, AnalyticsService.Aggregation.DAY);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(new BigDecimal("50"), result.getData().get(0).getValue());
    }

    @Test
    void testAggregateDate() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 8, 15);

        // When & Then
        assertEquals(testDate, analyticsService.aggregateDate(testDate, AnalyticsService.Aggregation.DAY));
        assertEquals(LocalDate.of(2025, 8, 1), analyticsService.aggregateDate(testDate, AnalyticsService.Aggregation.MONTH));
        assertEquals(LocalDate.of(2025, 1, 1), analyticsService.aggregateDate(testDate, AnalyticsService.Aggregation.YEAR));
        assertEquals(LocalDate.of(2025, 7, 1), analyticsService.aggregateDate(testDate, AnalyticsService.Aggregation.QUARTER));
    }

    @Test
    void testFirstDayOfQuarter() {
        // Given
        LocalDate q1Date = LocalDate.of(2025, 2, 15);
        LocalDate q2Date = LocalDate.of(2025, 5, 15);
        LocalDate q3Date = LocalDate.of(2025, 8, 15);
        LocalDate q4Date = LocalDate.of(2025, 11, 15);

        // When & Then
        assertEquals(LocalDate.of(2025, 1, 1), analyticsService.firstDayOfQuarter(q1Date));
        assertEquals(LocalDate.of(2025, 4, 1), analyticsService.firstDayOfQuarter(q2Date));
        assertEquals(LocalDate.of(2025, 7, 1), analyticsService.firstDayOfQuarter(q3Date));
        assertEquals(LocalDate.of(2025, 10, 1), analyticsService.firstDayOfQuarter(q4Date));
    }
}
