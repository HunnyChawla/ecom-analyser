package com.ecomanalyser.service;

import com.ecomanalyser.domain.ReturnTrackingEntity;
import com.ecomanalyser.repository.MergedOrderPaymentRepository;
import com.ecomanalyser.repository.ReturnTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReturnTrackingService {
    
    private static final Logger log = LoggerFactory.getLogger(ReturnTrackingService.class);
    
    @Autowired
    private ReturnTrackingRepository returnTrackingRepository;
    
    @Autowired
    private MergedOrderPaymentRepository mergedOrderRepository;
    
    /**
     * Sync return orders from merged_orders table
     * This method identifies orders with RETURN/RTO status and adds them to tracking
     */
    @Transactional
    public Map<String, Object> syncReturnOrders() {
        log.info("Starting return orders sync...");
        
        try {
            // Get all orders with RETURN or RTO status from merged_orders
            List<Object[]> returnOrders = mergedOrderRepository.findReturnOrdersForTracking();
            
            int addedCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;
            
            for (Object[] row : returnOrders) {
                String orderId = (String) row[0];
                String skuId = (String) row[1];
                Integer quantity = (Integer) row[2];
                BigDecimal returnAmount = (BigDecimal) row[3];
                String orderStatus = (String) row[4];
                LocalDate orderDate = (LocalDate) row[5];
                
                // Check if order already exists in tracking
                Optional<ReturnTrackingEntity> existingTracking = returnTrackingRepository.findByOrderId(orderId);
                
                if (existingTracking.isPresent()) {
                    // Update existing tracking record
                    ReturnTrackingEntity tracking = existingTracking.get();
                    tracking.setSkuId(skuId);
                    tracking.setQuantity(quantity);
                    tracking.setReturnAmount(returnAmount);
                    tracking.setOrderStatus(orderStatus);
                    tracking.setOrderDate(orderDate);
                    
                    returnTrackingRepository.save(tracking);
                    updatedCount++;
                    log.debug("Updated tracking for order: {}", orderId);
                } else {
                    // Create new tracking record
                    ReturnTrackingEntity newTracking = new ReturnTrackingEntity(
                        orderId, skuId, quantity, returnAmount, orderStatus, orderDate
                    );
                    
                    returnTrackingRepository.save(newTracking);
                    addedCount++;
                    log.debug("Added new tracking for order: {}", orderId);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("addedCount", addedCount);
            result.put("updatedCount", updatedCount);
            result.put("skippedCount", skippedCount);
            result.put("totalProcessed", returnOrders.size());
            
            log.info("Return orders sync completed. Added: {}, Updated: {}, Total: {}", 
                    addedCount, updatedCount, returnOrders.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error syncing return orders: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Mark an order as received
     */
    @Transactional
    public Map<String, Object> markOrderAsReceived(String orderId, String receivedBy, String notes) {
        log.info("Marking order {} as received by {}", orderId, receivedBy);
        
        try {
            Optional<ReturnTrackingEntity> trackingOpt = returnTrackingRepository.findByOrderId(orderId);
            
            if (trackingOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Order not found in return tracking: " + orderId);
                return result;
            }
            
            ReturnTrackingEntity tracking = trackingOpt.get();
            tracking.markAsReceived(receivedBy, notes);
            returnTrackingRepository.save(tracking);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Order marked as received successfully");
            result.put("orderId", orderId);
            result.put("receivedDate", tracking.getReceivedDate());
            result.put("receivedBy", receivedBy);
            
            log.info("Order {} marked as received successfully", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("Error marking order {} as received: {}", orderId, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Mark an order as not received
     */
    @Transactional
    public Map<String, Object> markOrderAsNotReceived(String orderId, String notes) {
        log.info("Marking order {} as not received", orderId);
        
        try {
            Optional<ReturnTrackingEntity> trackingOpt = returnTrackingRepository.findByOrderId(orderId);
            
            if (trackingOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Order not found in return tracking: " + orderId);
                return result;
            }
            
            ReturnTrackingEntity tracking = trackingOpt.get();
            tracking.markAsNotReceived(notes);
            returnTrackingRepository.save(tracking);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Order marked as not received successfully");
            result.put("orderId", orderId);
            result.put("updatedAt", tracking.getUpdatedAt());
            
            log.info("Order {} marked as not received successfully", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("Error marking order {} as not received: {}", orderId, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Search return orders with multiple criteria
     */
    public Map<String, Object> searchReturnOrders(String orderId, String skuId, 
                                                 LocalDate startDate, LocalDate endDate, 
                                                 ReturnTrackingEntity.ReturnStatus returnStatus) {
        log.info("Searching return orders with criteria: orderId={}, skuId={}, startDate={}, endDate={}, status={}", 
                orderId, skuId, startDate, endDate, returnStatus);
        
        try {
            List<ReturnTrackingEntity> allOrders;
            
            // Get base data based on return status
            if (returnStatus != null) {
                switch (returnStatus) {
                    case PENDING_RECEIPT:
                        allOrders = returnTrackingRepository.findPendingReceipts();
                        break;
                    case RECEIVED:
                        allOrders = returnTrackingRepository.findReceivedOrders();
                        break;
                    case NOT_RECEIVED:
                        allOrders = returnTrackingRepository.findNotReceivedOrders();
                        break;
                    default:
                        allOrders = returnTrackingRepository.findAll();
                }
            } else {
                allOrders = returnTrackingRepository.findAll();
            }
            
            // Apply filters
            List<ReturnTrackingEntity> filteredOrders = allOrders.stream()
                .filter(order -> {
                    // Filter by order ID
                    if (orderId != null && !orderId.trim().isEmpty()) {
                        if (!order.getOrderId().toLowerCase().contains(orderId.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by SKU ID
                    if (skuId != null && !skuId.trim().isEmpty()) {
                        if (!order.getSkuId().toLowerCase().contains(skuId.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by start date
                    if (startDate != null) {
                        if (order.getOrderDate().isBefore(startDate)) {
                            return false;
                        }
                    }
                    
                    // Filter by end date
                    if (endDate != null) {
                        if (order.getOrderDate().isAfter(endDate)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orders", filteredOrders);
            result.put("totalCount", filteredOrders.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error searching return orders: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Get return tracking summary
     */
    public Map<String, Object> getReturnTrackingSummary() {
        log.info("Getting return tracking summary");
        
        try {
            List<Object[]> statusCounts = returnTrackingRepository.countByReturnStatus();
            List<ReturnTrackingEntity> pendingReceipts = returnTrackingRepository.findPendingReceipts();
            List<ReturnTrackingEntity> receivedOrders = returnTrackingRepository.findReceivedOrders();
            List<ReturnTrackingEntity> notReceivedOrders = returnTrackingRepository.findNotReceivedOrders();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalOrders", pendingReceipts.size() + receivedOrders.size() + notReceivedOrders.size());
            summary.put("pendingReceipts", pendingReceipts.size());
            summary.put("receivedOrders", receivedOrders.size());
            summary.put("notReceivedOrders", notReceivedOrders.size());
            
            // Add status breakdown
            Map<String, Long> statusBreakdown = new HashMap<>();
            for (Object[] row : statusCounts) {
                String status = (String) row[0];
                Long count = (Long) row[1];
                statusBreakdown.put(status, count);
            }
            summary.put("statusBreakdown", statusBreakdown);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("summary", summary);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting return tracking summary: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Get orders by return status
     */
    public Map<String, Object> getOrdersByReturnStatus(ReturnTrackingEntity.ReturnStatus returnStatus) {
        log.info("Getting orders with return status: {}", returnStatus);
        
        try {
            List<ReturnTrackingEntity> orders;
            
            switch (returnStatus) {
                case PENDING_RECEIPT:
                    orders = returnTrackingRepository.findPendingReceipts();
                    break;
                case RECEIVED:
                    orders = returnTrackingRepository.findReceivedOrders();
                    break;
                case NOT_RECEIVED:
                    orders = returnTrackingRepository.findNotReceivedOrders();
                    break;
                default:
                    orders = returnTrackingRepository.findByReturnStatus(returnStatus);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orders", orders);
            result.put("totalCount", orders.size());
            result.put("returnStatus", returnStatus);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting orders by return status {}: {}", returnStatus, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Get order by order ID
     */
    public Map<String, Object> getOrderByOrderId(String orderId) {
        log.info("Getting return tracking for order: {}", orderId);
        
        try {
            Optional<ReturnTrackingEntity> trackingOpt = returnTrackingRepository.findByOrderId(orderId);
            
            Map<String, Object> result = new HashMap<>();
            if (trackingOpt.isPresent()) {
                result.put("success", true);
                result.put("order", trackingOpt.get());
            } else {
                result.put("success", false);
                result.put("error", "Order not found: " + orderId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
