package com.ecomanalyser.service;

import com.ecomanalyser.domain.NormalizedOrderEntity;
import com.ecomanalyser.domain.OrderRawEntity;
import com.ecomanalyser.repository.NormalizedOrderRepository;
import com.ecomanalyser.repository.OrderRawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNormalizationService {
    
    private final JobLauncher jobLauncher;
    @org.springframework.beans.factory.annotation.Qualifier("normalizeRawOrdersJob")
    private final Job normalizeRawOrdersJob;
    private final NormalizedOrderRepository normalizedOrderRepository;
    private final OrderRawRepository orderRawRepository;
    private final SkuResolverService skuResolverService;
    private final StatusNormalizationService statusNormalizationService;
    
    /**
     * Start the normalization job for a specific batch
     * @param batchId The batch ID to normalize
     * @return Job execution result
     */
    public JobExecution startNormalizationJob(String batchId) throws Exception {
        log.info("Starting normalization job for batch: {}", batchId);
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("batchId", batchId)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncher.run(normalizeRawOrdersJob, jobParameters);
        
        log.info("Started normalization job with execution ID: {}", jobExecution.getId());
        return jobExecution;
    }
    
    /**
     * Normalize orders manually without using Spring Batch (for smaller datasets)
     * @param batchId The batch ID to normalize
     * @return Normalization result summary
     */
    public Map<String, Object> normalizeOrdersManually(String batchId) {
        log.info("Starting manual normalization for batch: {}", batchId);
        
        try {
            // Clear existing normalized orders for this batch first
            clearNormalizedOrders(batchId);
            
            // Reset processed flags on raw orders
            List<OrderRawEntity> rawOrders = orderRawRepository.findByBatchIdAndValidationStatus(
                batchId, OrderRawEntity.ValidationStatus.VALID);
            
            if (rawOrders.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "No valid raw orders found for batch: " + batchId
                );
            }
            
            int processedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            
            for (OrderRawEntity rawOrder : rawOrders) {
                try {
                    // Skip legacy/bad rows that still contain CSVRecord object strings
                    String rawData = rawOrder.getRawData();
                    if (rawData != null && (rawData.contains("CSVRecord") || rawData.contains("recordNumber="))) {
                        skippedCount++;
                        log.warn("Skipping legacy raw row {} (contains CSVRecord object string)", rawOrder.getRowNumber());
                        continue;
                    }

                    Optional<NormalizedOrderEntity> normalizedOrder = processRawOrder(rawOrder);
                    
                    if (normalizedOrder.isPresent()) {
                        // Persist in its own transaction to avoid poisoning the whole batch
                        persistNormalizedOrderTransactional(normalizedOrder.get(), rawOrder);
                        processedCount++;
                        log.debug("Successfully normalized order {} from raw row {}", 
                                normalizedOrder.get().getOrderId(), rawOrder.getRowNumber());
                    } else {
                        skippedCount++;
                        log.debug("Skipped raw order row {}: returned null", rawOrder.getRowNumber());
                    }
                } catch (Exception e) {
                    log.error("Error processing raw order {}: {}", rawOrder.getRowNumber(), e.getMessage());
                    errorCount++;
                    continue;
                }
            }
            
            log.info("Manual normalization completed for batch {}: {} processed, {} skipped, {} errors", 
                    batchId, processedCount, skippedCount, errorCount);
            
            return Map.of(
                "success", true,
                "batchId", batchId,
                "processedCount", processedCount,
                "skippedCount", skippedCount,
                "errorCount", errorCount,
                "totalCount", rawOrders.size()
            );
            
        } catch (Exception e) {
            log.error("Error during manual normalization for batch {}: {}", batchId, e.getMessage());
            throw e;
        }
    }

    /**
     * Persist a normalized order and update its corresponding raw row in an independent transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void persistNormalizedOrderTransactional(NormalizedOrderEntity normalizedOrder, OrderRawEntity rawOrder) {
        // Upsert behavior: if an order with the same orderId exists, update it; otherwise create new
        normalizedOrderRepository.findByOrderId(normalizedOrder.getOrderId())
                .ifPresentOrElse(existing -> {
                    existing.setSku(normalizedOrder.getSku());
                    existing.setQuantity(normalizedOrder.getQuantity());
                    existing.setSellingPrice(normalizedOrder.getSellingPrice());
                    existing.setOrderDate(normalizedOrder.getOrderDate());
                    existing.setProductName(normalizedOrder.getProductName());
                    existing.setCustomerState(normalizedOrder.getCustomerState());
                    existing.setSize(normalizedOrder.getSize());
                    existing.setSupplierListedPrice(normalizedOrder.getSupplierListedPrice());
                    existing.setSupplierDiscountedPrice(normalizedOrder.getSupplierDiscountedPrice());
                    existing.setPacketId(normalizedOrder.getPacketId());
                    existing.setStandardizedStatus(normalizedOrder.getStandardizedStatus());
                    existing.setOriginalStatus(normalizedOrder.getOriginalStatus());
                    existing.setSupplierSku(normalizedOrder.getSupplierSku());
                    existing.setSkuResolved(normalizedOrder.getSkuResolved());
                    existing.setValidationErrors(normalizedOrder.getValidationErrors());
                    existing.setBatchId(normalizedOrder.getBatchId());
                    existing.setRawRowId(normalizedOrder.getRawRowId());
                    normalizedOrderRepository.save(existing);
                }, () -> {
                    normalizedOrderRepository.save(normalizedOrder);
                });

        // Mark raw order as processed
        rawOrder.setProcessed(true);
        orderRawRepository.save(rawOrder);
    }
    
    /**
     * Process a single raw order entity
     * @param rawOrder The raw order to process
     * @return Optional containing the normalized order, or empty if processing failed
     */
    private Optional<NormalizedOrderEntity> processRawOrder(OrderRawEntity rawOrder) {
        try {
            // Parse raw data (assuming CSV format)
            String[] fields = parseRawData(rawOrder.getRawData());
            
            if (fields.length < 11) {
                log.warn("Invalid raw data format for row {}: insufficient fields", rawOrder.getRowNumber());
                return Optional.empty();
            }
            
            // Extract fields from raw data
            String originalStatus = fields[0];
            String orderId = fields[1];
            String orderDateStr = fields[2];
            String customerState = fields[3];
            String productName = fields[4];
            String sku = fields[5];
            String size = fields[6];
            String quantityStr = fields[7];
            String supplierListedPriceStr = fields[8];
            String supplierDiscountedPriceStr = fields[9];
            String packetId = fields[10];
            
            // Validate required fields
            if (orderId == null || orderId.trim().isEmpty()) {
                log.warn("Missing order_id in row {}, skipping", rawOrder.getRowNumber());
                return Optional.empty();
            }
            
            // Process and normalize data
            NormalizedOrderEntity normalizedOrder = NormalizedOrderEntity.builder()
                    .orderId(orderId.trim())
                    .sku(resolveSku(sku, null)) // For now, no supplier_sku in orders
                    .quantity(parseInteger(quantityStr))
                    .sellingPrice(parseBigDecimal(supplierDiscountedPriceStr))
                    .orderDate(parseDate(orderDateStr))
                    .productName(productName)
                    .customerState(customerState)
                    .size(size)
                    .supplierListedPrice(parseBigDecimal(supplierListedPriceStr))
                    .supplierDiscountedPrice(parseBigDecimal(supplierDiscountedPriceStr))
                    .packetId(packetId)
                    .standardizedStatus(statusNormalizationService.normalizeStatus(originalStatus))
                    .originalStatus(originalStatus)
                    .supplierSku(null) // Not available in orders data
                    .skuResolved(sku != null && !sku.trim().isEmpty())
                    .validationErrors(null)
                    .batchId(rawOrder.getBatchId())
                    .rawRowId(rawOrder.getId())
                    .build();
            
            // Convert enum to string for database storage
            NormalizedOrderEntity finalOrder = NormalizedOrderEntity.builder()
                    .orderId(normalizedOrder.getOrderId())
                    .sku(normalizedOrder.getSku())
                    .quantity(normalizedOrder.getQuantity())
                    .sellingPrice(normalizedOrder.getSellingPrice())
                    .orderDate(normalizedOrder.getOrderDate())
                    .productName(normalizedOrder.getProductName())
                    .customerState(normalizedOrder.getCustomerState())
                    .size(normalizedOrder.getSize())
                    .supplierListedPrice(normalizedOrder.getSupplierListedPrice())
                    .supplierDiscountedPrice(normalizedOrder.getSupplierDiscountedPrice())
                    .packetId(normalizedOrder.getPacketId())
                    .standardizedStatus(normalizedOrder.getStandardizedStatus())
                    .originalStatus(normalizedOrder.getOriginalStatus())
                    .supplierSku(normalizedOrder.getSupplierSku())
                    .skuResolved(normalizedOrder.getSkuResolved())
                    .validationErrors(normalizedOrder.getValidationErrors())
                    .batchId(normalizedOrder.getBatchId())
                    .rawRowId(normalizedOrder.getRawRowId())
                    .build();
            
            return Optional.of(finalOrder);
            
        } catch (Exception e) {
            log.error("Error processing raw order row {}: {}", rawOrder.getRowNumber(), e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Get normalization statistics for a batch
     * @param batchId The batch ID
     * @return Statistics map
     */
    public Map<String, Object> getNormalizationStats(String batchId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalRawOrders = orderRawRepository.countTotalRowsByBatchId(batchId);
            long validRawOrders = orderRawRepository.countValidRowsByBatchId(batchId);
            long processedRawOrders = orderRawRepository.countByBatchIdAndProcessed(batchId, true);
            long normalizedOrders = normalizedOrderRepository.countByBatchId(batchId);
            
            stats.put("batchId", batchId);
            stats.put("totalRawOrders", totalRawOrders);
            stats.put("validRawOrders", validRawOrders);
            stats.put("processedRawOrders", processedRawOrders);
            stats.put("normalizedOrders", normalizedOrders);
            stats.put("processingProgress", validRawOrders > 0 ? (double) processedRawOrders / validRawOrders * 100 : 0);
            stats.put("normalizationProgress", validRawOrders > 0 ? (double) normalizedOrders / validRawOrders * 100 : 0);
            
            // Get status breakdown
            List<Object[]> statusCounts = normalizedOrderRepository.getStatusCountsByBatchId(batchId);
            Map<String, Long> statusBreakdown = new HashMap<>();
            for (Object[] statusCount : statusCounts) {
                statusBreakdown.put(statusCount[0].toString(), (Long) statusCount[1]);
            }
            stats.put("statusBreakdown", statusBreakdown);
            
            // Get SKU resolution stats
            long skuResolvedCount = normalizedOrderRepository.countByBatchIdAndSkuResolved(batchId, true);
            long skuUnresolvedCount = normalizedOrderRepository.countByBatchIdAndSkuResolved(batchId, false);
            stats.put("skuResolvedCount", skuResolvedCount);
            stats.put("skuUnresolvedCount", skuUnresolvedCount);
            stats.put("skuResolutionRate", validRawOrders > 0 ? (double) skuResolvedCount / validRawOrders * 100 : 0);
            
        } catch (Exception e) {
            log.error("Error getting normalization stats for batch {}: {}", batchId, e.getMessage());
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Clear normalized orders for a batch (useful for reprocessing)
     * @param batchId The batch ID
     * @return Success status
     */
    @Transactional
    public boolean clearNormalizedOrders(String batchId) {
        try {
            log.info("Clearing normalized orders for batch: {}", batchId);
            
            // Delete normalized orders
            normalizedOrderRepository.deleteByBatchId(batchId);
            
            // Reset processed flag on raw orders
            List<OrderRawEntity> rawOrders = orderRawRepository.findByBatchId(batchId);
            for (OrderRawEntity rawOrder : rawOrders) {
                rawOrder.setProcessed(false);
                rawOrder.setValidationErrors(null);
                orderRawRepository.save(rawOrder);
            }
            
            log.info("Successfully cleared normalized orders for batch: {}", batchId);
            return true;
            
        } catch (Exception e) {
            log.error("Error clearing normalized orders for batch {}: {}", batchId, e.getMessage());
            return false;
        }
    }
    
    // Helper methods (same as in BatchConfig)
    private String[] parseRawData(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return new String[0];
        }
        
        // Split by comma and handle empty fields
        String[] fields = rawData.split(",");
        
        // Ensure we have at least 11 fields by padding with empty strings if needed
        String[] paddedFields = new String[11];
        for (int i = 0; i < 11; i++) {
            if (i < fields.length) {
                paddedFields[i] = fields[i] != null ? fields[i].trim() : "";
            } else {
                paddedFields[i] = "";
            }
        }
        
        return paddedFields;
    }
    
    private String resolveSku(String sku, String supplierSku) {
        return skuResolverService.resolveSku(sku, supplierSku);
    }
    
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse integer: {}", value);
            return null;
        }
    }
    
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse decimal: {}", value);
            return null;
        }
    }
    
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date: {}", value);
            return null;
        }
    }
}
