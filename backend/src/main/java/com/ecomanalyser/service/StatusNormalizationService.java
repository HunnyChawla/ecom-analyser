package com.ecomanalyser.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StatusNormalizationService {
    
    // Mapping of various status formats to standardized codes
    private static final Map<String, String> STATUS_MAPPING = new HashMap<>();
    
    static {
        // Direct mappings
        STATUS_MAPPING.put("DELIVERED", "DELIVERED");
        STATUS_MAPPING.put("delivered", "DELIVERED");
        STATUS_MAPPING.put("Delivered", "DELIVERED");
        
        STATUS_MAPPING.put("SHIPPED", "SHIPPED");
        STATUS_MAPPING.put("shipped", "SHIPPED");
        STATUS_MAPPING.put("Shipped", "SHIPPED");
        
        STATUS_MAPPING.put("PENDING", "PENDING");
        STATUS_MAPPING.put("pending", "PENDING");
        STATUS_MAPPING.put("Pending", "PENDING");
        
        STATUS_MAPPING.put("CANCELLED", "CANCELLED");
        STATUS_MAPPING.put("cancelled", "CANCELLED");
        STATUS_MAPPING.put("Cancelled", "CANCELLED");
        STATUS_MAPPING.put("CANCEL", "CANCELLED");
        STATUS_MAPPING.put("cancel", "CANCELLED");
        
        STATUS_MAPPING.put("RTO_COMPLETE", "RTO_COMPLETE");
        STATUS_MAPPING.put("rto_complete", "RTO_COMPLETE");
        STATUS_MAPPING.put("RTO Complete", "RTO_COMPLETE");
        STATUS_MAPPING.put("RTO", "RTO_COMPLETE");
        STATUS_MAPPING.put("rto", "RTO_COMPLETE");
        
        STATUS_MAPPING.put("RETURNED", "RETURNED");
        STATUS_MAPPING.put("returned", "RETURNED");
        STATUS_MAPPING.put("Returned", "RETURNED");
        STATUS_MAPPING.put("RETURN", "RETURNED");
        STATUS_MAPPING.put("return", "RETURNED");
        
        STATUS_MAPPING.put("REFUNDED", "REFUNDED");
        STATUS_MAPPING.put("refunded", "REFUNDED");
        STATUS_MAPPING.put("Refunded", "REFUNDED");
        STATUS_MAPPING.put("REFUND", "REFUNDED");
        STATUS_MAPPING.put("refund", "REFUNDED");
        
        STATUS_MAPPING.put("EXCHANGE", "EXCHANGE");
        STATUS_MAPPING.put("exchange", "EXCHANGE");
        STATUS_MAPPING.put("Exchange", "EXCHANGE");
        
        // Common variations and abbreviations
        STATUS_MAPPING.put("IN_TRANSIT", "SHIPPED");
        STATUS_MAPPING.put("in_transit", "SHIPPED");
        STATUS_MAPPING.put("In Transit", "SHIPPED");
        STATUS_MAPPING.put("IN TRANSIT", "SHIPPED");
        
        STATUS_MAPPING.put("OUT_FOR_DELIVERY", "SHIPPED");
        STATUS_MAPPING.put("out_for_delivery", "SHIPPED");
        STATUS_MAPPING.put("Out For Delivery", "SHIPPED");
        STATUS_MAPPING.put("OUT FOR DELIVERY", "SHIPPED");
        
        STATUS_MAPPING.put("PROCESSING", "PENDING");
        STATUS_MAPPING.put("processing", "PENDING");
        STATUS_MAPPING.put("Processing", "PENDING");
        
        STATUS_MAPPING.put("CONFIRMED", "PENDING");
        STATUS_MAPPING.put("confirmed", "PENDING");
        STATUS_MAPPING.put("Confirmed", "PENDING");
    }
    
    /**
     * Normalize order status to standardized string value
     * @param originalStatus The original status string from the raw data
     * @return Standardized status string
     */
    public String normalizeStatus(String originalStatus) {
        if (originalStatus == null || originalStatus.trim().isEmpty()) {
            log.warn("Null or empty status provided, defaulting to UNKNOWN");
            return "UNKNOWN";
        }
        
        String trimmedStatus = originalStatus.trim();
        
        // Check direct mapping first
        if (STATUS_MAPPING.containsKey(trimmedStatus)) {
            log.debug("Status '{}' normalized to '{}'", trimmedStatus, STATUS_MAPPING.get(trimmedStatus));
            return STATUS_MAPPING.get(trimmedStatus);
        }
        
        // Try case-insensitive matching
        for (Map.Entry<String, String> entry : STATUS_MAPPING.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(trimmedStatus)) {
                log.debug("Status '{}' normalized to '{}' (case-insensitive match)", trimmedStatus, entry.getValue());
                return entry.getValue();
            }
        }
        
        // Try partial matching for common patterns
        String upperStatus = trimmedStatus.toUpperCase();
        if (upperStatus.contains("DELIVER")) {
            log.debug("Status '{}' normalized to '{}' (partial match: DELIVER)", trimmedStatus, "DELIVERED");
            return "DELIVERED";
        }
        if (upperStatus.contains("SHIP") || upperStatus.contains("TRANSIT")) {
            log.debug("Status '{}' normalized to '{}' (partial match: SHIP/TRANSIT)", trimmedStatus, "SHIPPED");
            return "SHIPPED";
        }
        if (upperStatus.contains("PEND") || upperStatus.contains("PROCESS") || upperStatus.contains("CONFIRM")) {
            log.debug("Status '{}' normalized to '{}' (partial match: PEND/PROCESS/CONFIRM)", trimmedStatus, "PENDING");
            return "PENDING";
        }
        if (upperStatus.contains("CANCEL")) {
            log.debug("Status '{}' normalized to '{}' (partial match: CANCEL)", trimmedStatus, "CANCELLED");
            return "CANCELLED";
        }
        if (upperStatus.contains("RTO")) {
            log.debug("Status '{}' normalized to '{}' (partial match: RTO)", trimmedStatus, "RTO_COMPLETE");
            return "RTO_COMPLETE";
        }
        if (upperStatus.contains("RETURN")) {
            log.debug("Status '{}' normalized to '{}' (partial match: RETURN)", trimmedStatus, "RETURNED");
            return "RETURNED";
        }
        if (upperStatus.contains("REFUND")) {
            log.debug("Status '{}' normalized to '{}' (partial match: REFUND)", trimmedStatus, "REFUNDED");
            return "REFUNDED";
        }
        if (upperStatus.contains("EXCHANGE")) {
            log.debug("Status '{}' normalized to '{}' (partial match: EXCHANGE)", trimmedStatus, "EXCHANGE");
            return "EXCHANGE";
        }
        
        // No match found, log warning and return UNKNOWN
        log.warn("Could not normalize status '{}', defaulting to UNKNOWN", trimmedStatus);
        return "UNKNOWN";
    }
    
    /**
     * Get all possible standardized statuses
     * @return Array of all standardized status strings
     */
    public String[] getAllStandardizedStatuses() {
        return new String[]{"PENDING", "SHIPPED", "DELIVERED", "CANCELLED", "RTO_COMPLETE", "RETURNED", "REFUNDED", "EXCHANGE", "UNKNOWN"};
    }
    
    /**
     * Get status mapping statistics
     * @return Map containing mapping statistics
     */
    public Map<String, Object> getMappingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMappings", STATUS_MAPPING.size());
        stats.put("uniqueStandardizedStatuses", STATUS_MAPPING.values().stream().distinct().count());
        return stats;
    }
}
