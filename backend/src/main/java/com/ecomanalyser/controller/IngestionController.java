package com.ecomanalyser.controller;

import com.ecomanalyser.dto.IngestionResponse;
import com.ecomanalyser.service.FileIngestionService;
import com.ecomanalyser.event.FileIngestedEvent.FileType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Ingestion", description = "File upload and ingestion endpoints")
public class IngestionController {
    
    private final FileIngestionService fileIngestionService;
    
    @PostMapping("/upload")
    @Operation(
        summary = "Upload and ingest file",
        description = "Upload orders.csv or payments.xlsx file for ingestion into staging tables"
    )
    public ResponseEntity<IngestionResponse> uploadFile(
            @Parameter(description = "File type: ORDERS or PAYMENTS", required = true)
            @RequestParam("type") String type,
            @Parameter(description = "CSV or XLSX file to upload", required = true)
            @RequestPart("file") MultipartFile file) {
        
        log.info("File upload request received - type: {}, file: {}, size: {} bytes", 
                type, file.getOriginalFilename(), file.getSize());
        
        try {
            // Validate file type parameter
            FileType fileType;
            try {
                fileType = FileType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid file type: {}", type);
                return ResponseEntity.badRequest().build();
            }
            
            // Validate file
            if (file.isEmpty()) {
                log.error("Uploaded file is empty");
                return ResponseEntity.badRequest().build();
            }
            
            // Process file ingestion
            IngestionResponse response = fileIngestionService.ingestFile(file, fileType);
            
            log.info("File ingestion completed - batch: {}, accepted: {}, rejected: {}, warnings: {}", 
                    response.getBatchId(), response.getAcceptedRows(), response.getRejectedRows(), response.getWarningsCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during file upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if ingestion service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ingestion service is healthy");
    }
}
