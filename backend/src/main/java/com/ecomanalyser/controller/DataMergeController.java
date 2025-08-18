package com.ecomanalyser.controller;

import com.ecomanalyser.service.DataMergeService;
import com.ecomanalyser.domain.MergedOrderPaymentEntity;
import com.ecomanalyser.repository.MergedOrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/data-merge")
@RequiredArgsConstructor
@Slf4j
public class DataMergeController {

    private final DataMergeService dataMergeService;
    private final MergedOrderPaymentRepository mergedOrderPaymentRepository;

    /**
     * Get merged orders and payments data from merged_orders table
     */
    @GetMapping("/merged-data")
    public ResponseEntity<List<Map<String, Object>>> getMergedData() {
        try {
            log.info("Requesting merged orders and payments data from merged_orders table");
            List<MergedOrderPaymentEntity> mergedData = mergedOrderPaymentRepository.findAllFromMergedOrders();
            
            // Map to UI-expected field names
            List<Map<String, Object>> mappedData = mergedData.stream()
                    .map(entity -> {
                        Map<String, Object> mapped = new HashMap<>();
                        mapped.put("orderId", entity.getOrderId());
                        mapped.put("sku", entity.getSkuId());
                        mapped.put("productName", "N/A"); // Not available in merged_orders table
                        mapped.put("finalStatus", entity.getOrderStatus());
                        mapped.put("statusSource", "MERGED_TABLE"); // Default source
                        mapped.put("amount", entity.getSettlementAmount());
                        mapped.put("quantity", entity.getQuantity());
                        mapped.put("orderDateTime", entity.getOrderDate() != null ? entity.getOrderDate().atStartOfDay() : null);
                        return mapped;
                    })
                    .toList();
            
            log.info("Successfully retrieved {} merged records from merged_orders table", mappedData.size());
            return ResponseEntity.ok(mappedData);
        } catch (Exception e) {
            log.error("Error retrieving merged data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get merge statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMergeStatistics() {
        try {
            log.info("Requesting merge statistics");
            Map<String, Object> stats = dataMergeService.getMergeStatistics();
            
            log.info("Successfully retrieved merge statistics");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving merge statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Rebuild merged table from orders and payments
     */
    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildMergedTable() {
        try {
            int count = dataMergeService.rebuildMergedTable();
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Merged table rebuilt",
                    "records", count
            ));
        } catch (Exception e) {
            log.error("Error rebuilding merged table: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simple test endpoint to check if data can be retrieved
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        try {
            log.info("Testing simple endpoint...");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Test successful");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in test endpoint: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get merged data with pagination
     */
    @GetMapping("/merged-data/paginated")
    public ResponseEntity<Map<String, Object>> getMergedDataPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "q", required = false) String query) {
        try {
            log.info("Requesting merged data with pagination - page: {}, size: {}, query: {}", page, size, query);

            // Read from merged_orders for consistency
            List<MergedOrderPaymentEntity> allEntities = mergedOrderPaymentRepository.findAllFromMergedOrders();

            // Optional search across orderId, skuId and orderStatus
            List<MergedOrderPaymentEntity> filteredEntities = allEntities;
            if (query != null && !query.trim().isEmpty()) {
                String qLower = query.trim().toLowerCase();
                filteredEntities = allEntities.stream()
                        .filter(e ->
                                (e.getOrderId() != null && e.getOrderId().toLowerCase().contains(qLower)) ||
                                (e.getSkuId() != null && e.getSkuId().toLowerCase().contains(qLower)) ||
                                (e.getOrderStatus() != null && e.getOrderStatus().toLowerCase().contains(qLower))
                        )
                        .toList();
            }

            int totalRecords = filteredEntities.size();
            int totalPages = (int) Math.ceil((double) totalRecords / size);
            
            // Validate page number
            if (page < 0) {
                page = 0;
            }
            if (page >= totalPages && totalPages > 0) {
                page = totalPages - 1;
            }
            
            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalRecords);
            
            // Ensure indices are valid
            if (startIndex >= totalRecords) {
                startIndex = 0;
                endIndex = Math.min(size, totalRecords);
            }
            
            List<MergedOrderPaymentEntity> pageSlice = filteredEntities.subList(startIndex, endIndex);

            // Map to UI-expected structure
            List<Map<String, Object>> paginatedData = pageSlice.stream()
                    .map(entity -> {
                        Map<String, Object> mapped = new HashMap<>();
                        mapped.put("orderId", entity.getOrderId());
                        mapped.put("sku", entity.getSkuId());
                        mapped.put("productName", "N/A");
                        mapped.put("finalStatus", entity.getOrderStatus());
                        mapped.put("statusSource", "MERGED_TABLE");
                        mapped.put("amount", entity.getSettlementAmount());
                        mapped.put("quantity", entity.getQuantity());
                        mapped.put("orderDateTime", entity.getOrderDate() != null ? entity.getOrderDate().atStartOfDay() : null);
                        return mapped;
                    })
                    .toList();
            
            // Build response with full data structure
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedData);
            response.put("totalRecords", totalRecords);
            response.put("pageSize", size);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            response.put("hasNext", page < totalPages - 1);
            response.put("hasPrevious", page > 0);
            
            log.info("Successfully retrieved paginated merged data - page {} of {} (filteredRecords={})", page + 1, totalPages, totalRecords);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving paginated merged data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get merged data filtered by final status
     */
    @GetMapping("/merged-data/status/{status}")
    public ResponseEntity<Map<String, Object>> getMergedDataByStatus(
            @PathVariable("status") String status) {
        try {
            log.info("Requesting merged data filtered by status: {}", status);
            
            if (status == null || status.trim().isEmpty()) {
                log.warn("Status parameter is null or empty");
                return ResponseEntity.badRequest().build();
            }
            
            // Use the merged_orders table directly for consistency and performance
            List<MergedOrderPaymentEntity> allData = mergedOrderPaymentRepository.findAllFromMergedOrders();
            List<MergedOrderPaymentEntity> filteredData = allData.stream()
                    .filter(record -> record.getOrderStatus() != null && 
                                   status.equalsIgnoreCase(record.getOrderStatus().trim()))
                    .toList();
            
            // Map to UI-expected field names
            List<Map<String, Object>> mappedData = filteredData.stream()
                    .map(entity -> {
                        Map<String, Object> mapped = new HashMap<>();
                        mapped.put("orderId", entity.getOrderId());
                        mapped.put("sku", entity.getSkuId());
                        mapped.put("productName", "N/A"); // Not available in merged_orders table
                        mapped.put("finalStatus", entity.getOrderStatus());
                        mapped.put("statusSource", "MERGED_TABLE"); // Default source
                        mapped.put("amount", entity.getSettlementAmount());
                        mapped.put("quantity", entity.getQuantity());
                        mapped.put("orderDateTime", entity.getOrderDate() != null ? entity.getOrderDate().atStartOfDay() : null);
                        return mapped;
                    })
                    .toList();
            
            // Build consistent response structure matching the paginated endpoint
            Map<String, Object> response = new HashMap<>();
            response.put("data", mappedData);
            response.put("totalRecords", mappedData.size());
            response.put("pageSize", mappedData.size());
            response.put("currentPage", 0);
            response.put("totalPages", 1);
            response.put("hasNext", false);
            response.put("hasPrevious", false);
            response.put("status", status);
            
            log.info("Found {} records with status: {}", filteredData.size(), status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving merged data by status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get merged data filtered by status source
     */
    @GetMapping("/merged-data/source/{source}")
    public ResponseEntity<List<DataMergeService.MergedOrderData>> getMergedDataBySource(
            @PathVariable String source) {
        try {
            log.info("Requesting merged data filtered by source: {}", source);
            
            if (source == null || source.trim().isEmpty()) {
                log.warn("Source parameter is null or empty");
                return ResponseEntity.badRequest().build();
            }
            
            List<DataMergeService.MergedOrderData> allData = dataMergeService.mergeOrdersAndPayments();
            List<DataMergeService.MergedOrderData> filteredData = allData.stream()
                    .filter(record -> record.getStatusSource() != null && 
                                   source.equalsIgnoreCase(record.getStatusSource().trim()))
                    .toList();
            
            log.info("Found {} records with source: {}", filteredData.size(), source);
            return ResponseEntity.ok(filteredData);
        } catch (Exception e) {
            log.error("Error retrieving merged data by source: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
