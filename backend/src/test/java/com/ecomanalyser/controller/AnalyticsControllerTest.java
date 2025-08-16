package com.ecomanalyser.controller;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.service.AnalyticsService;
import com.ecomanalyser.repository.OrderRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AnalyticsController analyticsController;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2025, 8, 1);
        endDate = LocalDate.of(2025, 8, 15);
    }

    @Test
    void testTopOrdered() {
        // Given
        List<Map<String, Object>> mockData = Arrays.asList(
            Map.of("sku", "SKU1", "quantity", 10L),
            Map.of("sku", "SKU2", "quantity", 5L)
        );
        
        when(analyticsService.topOrderedSkus(startDate, endDate, 10))
                .thenReturn(mockData);

        // When
        List<Map<String, Object>> result = analyticsController.topOrdered(startDate, endDate, 10);

        // Then
        assertEquals(2, result.size());
        assertEquals("SKU1", result.get(0).get("sku"));
        assertEquals(10L, result.get(0).get("quantity"));
        assertEquals("SKU2", result.get(1).get("sku"));
        assertEquals(5L, result.get(1).get("quantity"));
    }

    @Test
    void testTopProfitableSkus() {
        // Given
        List<Map<String, Object>> mockData = Arrays.asList(
            Map.of("sku", "SKU1", "profit", new BigDecimal("100")),
            Map.of("sku", "SKU2", "profit", new BigDecimal("50"))
        );
        
        when(analyticsService.topProfitableSkus(startDate, endDate, 10))
                .thenReturn(mockData);

        // When
        List<Map<String, Object>> result = analyticsController.topProfitableSkus(startDate, endDate, 10);

        // Then
        assertEquals(2, result.size());
        assertEquals("SKU1", result.get(0).get("sku"));
        assertEquals(new BigDecimal("100"), result.get(0).get("profit"));
        assertEquals("SKU2", result.get(1).get("sku"));
        assertEquals(new BigDecimal("50"), result.get(1).get("profit"));
    }

    @Test
    void testGetOrderCountsByStatus() {
        // Given
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        List<Map<String, Object>> expectedData = List.of(
            Map.of("status", "Delivered", "count", 100L),
            Map.of("status", "RTO", "count", 10L)
        );
        
        when(analyticsService.getOrderCountsByStatus(start, end)).thenReturn(expectedData);
        
        // When
        ResponseEntity<List<Map<String, Object>>> response = analyticsController.getOrderCountsByStatus(start, end);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedData, response.getBody());
        Mockito.verify(analyticsService).getOrderCountsByStatus(start, end);
    }

    @Test
    void testDebugOrders() {
        // Given
        OrderEntity order1 = OrderEntity.builder()
                .orderId("ORDER1")
                .sku("SKU1")
                .orderDateTime(LocalDateTime.of(2025, 8, 1, 10, 0))
                .build();
        OrderEntity order2 = OrderEntity.builder()
                .orderId("ORDER2")
                .sku("SKU2")
                .orderDateTime(LocalDateTime.of(2025, 8, 15, 14, 0))
                .build();

        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));

        // When
        Map<String, Object> result = analyticsController.debugOrders();

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals(2, result.get("totalOrders"));
        assertEquals("ORDER1", result.get("firstOrderSku"));
        assertEquals("ORDER2", result.get("lastOrderSku"));
        assertNotNull(result.get("firstOrderDate"));
        assertNotNull(result.get("lastOrderDate"));
    }

    @Test
    void testDebugOrdersWithEmptyData() {
        // Given
        when(orderRepository.findAll()).thenReturn(List.of());

        // When
        Map<String, Object> result = analyticsController.debugOrders();

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals(0, result.get("totalOrders"));
        assertNull(result.get("firstOrderSku"));
        assertNull(result.get("lastOrderSku"));
    }

    @Test
    void testDebugOrdersWithException() {
        // Given
        when(orderRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // When
        Map<String, Object> result = analyticsController.debugOrders();

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("Database error", result.get("error"));
    }
}
