package com.ecomanalyser.controller;

import com.ecomanalyser.service.OrderNormalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order-normalization")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Normalization", description = "Endpoints for normalizing raw order data")
public class OrderNormalizationController {
    
    private final OrderNormalizationService orderNormalizationService;
    
    @PostMapping("/start-job/{batchId}")
    @Operation(
        summary = "Start normalization job",
        description = "Start a Spring Batch job to normalize raw orders for a specific batch"
    )
    public ResponseEntity<Map<String, Object>> startNormalizationJob(
            @Parameter(description = "Batch ID to normalize", required = true)
            @PathVariable("batchId") String batchId) {
        
        try {
            log.info("Starting normalization job for batch: {}", batchId);
            JobExecution jobExecution = orderNormalizationService.startNormalizationJob(batchId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "batchId", batchId,
                "jobExecutionId", jobExecution.getId(),
                "status", jobExecution.getStatus().toString(),
                "startTime", jobExecution.getStartTime()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error starting normalization job for batch {}: {}", batchId, e.getMessage());
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "batchId", batchId,
                "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/manual/{batchId}")
    @Operation(
        summary = "Normalize orders manually",
        description = "Normalize raw orders manually without using Spring Batch (for smaller datasets)"
    )
    public ResponseEntity<Map<String, Object>> normalizeOrdersManually(
            @Parameter(description = "Batch ID to normalize", required = true)
            @PathVariable("batchId") String batchId) {
        
        try {
            log.info("Starting manual normalization for batch: {}", batchId);
            Map<String, Object> result = orderNormalizationService.normalizeOrdersManually(batchId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("Error during manual normalization for batch {}: {}", batchId, e.getMessage());
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "batchId", batchId,
                "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/stats/{batchId}")
    @Operation(
        summary = "Get normalization statistics",
        description = "Get detailed statistics about the normalization process for a specific batch"
    )
    public ResponseEntity<Map<String, Object>> getNormalizationStats(
            @Parameter(description = "Batch ID to get stats for", required = true)
            @PathVariable("batchId") String batchId) {
        
        try {
            Map<String, Object> stats = orderNormalizationService.getNormalizationStats(batchId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting normalization stats for batch {}: {}", batchId, e.getMessage());
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "batchId", batchId,
                "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @DeleteMapping("/clear/{batchId}")
    @Operation(
        summary = "Clear normalized orders",
        description = "Clear all normalized orders for a batch and reset processing flags (useful for reprocessing)"
    )
    public ResponseEntity<Map<String, Object>> clearNormalizedOrders(
            @Parameter(description = "Batch ID to clear", required = true)
            @PathVariable("batchId") String batchId) {
        
        try {
            boolean success = orderNormalizationService.clearNormalizedOrders(batchId);
            
            if (success) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "batchId", batchId,
                    "message", "Successfully cleared normalized orders"
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "batchId", batchId,
                    "error", "Failed to clear normalized orders"
                );
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("Error clearing normalized orders for batch {}: {}", batchId, e.getMessage());
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "batchId", batchId,
                "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the normalization service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order normalization service is healthy");
    }
}
