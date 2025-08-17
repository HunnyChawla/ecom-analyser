package com.ecomanalyser.service;

import com.ecomanalyser.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkuResolverService {
    
    private final OrderRepository orderRepository;
    
    // Cache for SKU mappings to improve performance
    private final Map<String, String> skuCache = new HashMap<>();
    
    /**
     * Resolve SKU from supplier_sku
     * @param supplierSku The supplier SKU to resolve
     * @return Optional containing the resolved SKU, or empty if not found
     */
    public Optional<String> resolveSkuFromSupplierSku(String supplierSku) {
        if (supplierSku == null || supplierSku.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Check cache first
        if (skuCache.containsKey(supplierSku)) {
            return Optional.of(skuCache.get(supplierSku));
        }
        
        try {
            // Try to find existing order with this supplier_sku
            Optional<String> resolvedSku = orderRepository.findBySupplierSku(supplierSku.trim())
                    .map(order -> order.getSku());
            
            // Cache the result (even if not found, to avoid repeated DB calls)
            skuCache.put(supplierSku, resolvedSku.orElse(null));
            
            if (resolvedSku.isPresent()) {
                log.debug("Resolved SKU '{}' from supplier_sku '{}'", resolvedSku.get(), supplierSku);
            } else {
                log.debug("Could not resolve SKU from supplier_sku '{}'", supplierSku);
            }
            
            return resolvedSku;
            
        } catch (Exception e) {
            log.error("Error resolving SKU from supplier_sku '{}': {}", supplierSku, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Resolve SKU from multiple sources (direct SKU, supplier_sku, or generate placeholder)
     * @param sku Direct SKU if available
     * @param supplierSku Supplier SKU as fallback
     * @return Resolved SKU string
     */
    public String resolveSku(String sku, String supplierSku) {
        // If direct SKU is available and valid, use it
        if (sku != null && !sku.trim().isEmpty()) {
            return sku.trim();
        }
        
        // Try to resolve from supplier_sku
        Optional<String> resolvedSku = resolveSkuFromSupplierSku(supplierSku);
        if (resolvedSku.isPresent()) {
            return resolvedSku.get();
        }
        
        // Generate placeholder SKU if neither is available
        String placeholderSku = generatePlaceholderSku(supplierSku);
        log.warn("Generated placeholder SKU '{}' for supplier_sku '{}'", placeholderSku, supplierSku);
        return placeholderSku;
    }
    
    /**
     * Generate a placeholder SKU when no mapping is available
     * @param supplierSku The supplier SKU to base the placeholder on
     * @return Generated placeholder SKU
     */
    private String generatePlaceholderSku(String supplierSku) {
        if (supplierSku != null && !supplierSku.trim().isEmpty()) {
            return "PLACEHOLDER_" + supplierSku.trim().replaceAll("[^a-zA-Z0-9]", "_");
        }
        return "PLACEHOLDER_UNKNOWN_" + System.currentTimeMillis();
    }
    
    /**
     * Clear the SKU cache (useful for testing or when data changes)
     */
    public void clearCache() {
        skuCache.clear();
        log.info("SKU cache cleared");
    }
    
    /**
     * Get cache statistics
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", skuCache.size());
        stats.put("cachedEntries", skuCache.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .count());
        stats.put("nullEntries", skuCache.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .count());
        return stats;
    }
}

