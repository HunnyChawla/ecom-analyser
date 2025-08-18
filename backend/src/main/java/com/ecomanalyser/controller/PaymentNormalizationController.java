package com.ecomanalyser.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment-normalization")
@Slf4j
@Tag(name = "Payment Normalization", description = "Endpoints for normalizing raw payment data")
public class PaymentNormalizationController {

    private final JobLauncher jobLauncher;
    private final Job normalizeRawPaymentsJob;

    public PaymentNormalizationController(
            JobLauncher jobLauncher,
            @Qualifier("normalizeRawPaymentsJob") Job normalizeRawPaymentsJob
    ) {
        this.jobLauncher = jobLauncher;
        this.normalizeRawPaymentsJob = normalizeRawPaymentsJob;
    }

    @PostMapping("/start-job/{batchId}")
    @Operation(
        summary = "Start payment normalization job",
        description = "Start a Spring Batch job to normalize raw payments for a specific batch (currently processes all VALID, unprocessed rows)"
    )
    public ResponseEntity<Map<String, Object>> startPaymentNormalizationJob(
            @Parameter(description = "Batch ID to normalize", required = true)
            @PathVariable("batchId") String batchId) {
        try {
            log.info("Starting payment normalization job for batch: {}", batchId);
            JobParameters params = new JobParametersBuilder()
                    .addString("batchId", batchId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution exec = jobLauncher.run(normalizeRawPaymentsJob, params);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "batchId", batchId,
                    "jobExecutionId", exec.getId(),
                    "status", exec.getStatus().toString(),
                    "startTime", exec.getStartTime()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting payment normalization job for batch {}: {}", batchId, e.getMessage());
            Map<String, Object> error = Map.of(
                    "success", false,
                    "batchId", batchId,
                    "error", e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the payment normalization service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment normalization service is healthy");
    }
}


