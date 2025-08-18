package com.ecomanalyser.controller;

import com.ecomanalyser.domain.ReturnTrackingEntity;
import com.ecomanalyser.service.ReturnTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/return-tracking")
@CrossOrigin(origins = "*")
public class ReturnTrackingController {
    
    private static final Logger log = LoggerFactory.getLogger(ReturnTrackingController.class);
    
    @Autowired
    private ReturnTrackingService returnTrackingService;
    
    /**
     * Sync return orders from merged_orders table
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncReturnOrders() {
        log.info("Return tracking sync requested");
        
        try {
            Map<String, Object> result = returnTrackingService.syncReturnOrders();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in return tracking sync: {}", e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to sync return orders: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Mark an order as received
     */
    @PostMapping("/mark-received")
    public ResponseEntity<Map<String, Object>> markOrderAsReceived(
            @RequestParam("orderId") String orderId,
            @RequestParam("receivedBy") String receivedBy,
            @RequestParam(value = "notes", required = false) String notes) {
        
        log.info("Mark order as received requested: orderId={}, receivedBy={}", orderId, receivedBy);
        
        try {
            Map<String, Object> result = returnTrackingService.markOrderAsReceived(orderId, receivedBy, notes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error marking order {} as received: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to mark order as received: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Mark an order as not received
     */
    @PostMapping("/mark-not-received")
    public ResponseEntity<Map<String, Object>> markOrderAsNotReceived(
            @RequestParam("orderId") String orderId,
            @RequestParam("notes") String notes) {
        
        log.info("Mark order as not received requested: orderId={}", orderId);
        
        try {
            Map<String, Object> result = returnTrackingService.markOrderAsNotReceived(orderId, notes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error marking order {} as not received: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to mark order as not received: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Search return orders with multiple criteria
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchReturnOrders(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "skuId", required = false) String skuId,
            @RequestParam(value = "start", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "returnStatus", required = false) String returnStatus) {
        
        log.info("Search return orders requested: orderId={}, skuId={}, startDate={}, endDate={}, status={}", 
                orderId, skuId, startDate, endDate, returnStatus);
        
        try {
            ReturnTrackingEntity.ReturnStatus status = null;
            if (returnStatus != null && !returnStatus.trim().isEmpty()) {
                try {
                    status = ReturnTrackingEntity.ReturnStatus.valueOf(returnStatus.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid return status: {}", returnStatus);
                }
            }
            
            Map<String, Object> result = returnTrackingService.searchReturnOrders(orderId, skuId, startDate, endDate, status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error searching return orders: {}", e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to search return orders: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Get return tracking summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReturnTrackingSummary() {
        log.info("Return tracking summary requested");
        
        try {
            Map<String, Object> result = returnTrackingService.getReturnTrackingSummary();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting return tracking summary: {}", e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to get return tracking summary: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Get orders by return status
     */
    @GetMapping("/status/{returnStatus}")
    public ResponseEntity<Map<String, Object>> getOrdersByReturnStatus(
            @PathVariable("returnStatus") String returnStatus) {
        
        log.info("Get orders by return status requested: {}", returnStatus);
        
        try {
            ReturnTrackingEntity.ReturnStatus status;
            try {
                status = ReturnTrackingEntity.ReturnStatus.valueOf(returnStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "error", "Invalid return status: " + returnStatus
                );
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            Map<String, Object> result = returnTrackingService.getOrdersByReturnStatus(status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting orders by return status {}: {}", returnStatus, e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to get orders by return status: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Get order by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderByOrderId(@PathVariable("orderId") String orderId) {
        log.info("Get return tracking for order requested: {}", orderId);
        
        try {
            Map<String, Object> result = returnTrackingService.getOrderByOrderId(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", "Failed to get order: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}
