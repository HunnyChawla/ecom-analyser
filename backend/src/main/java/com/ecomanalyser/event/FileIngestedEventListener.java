package com.ecomanalyser.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileIngestedEventListener {
    
    @EventListener
    public void handleFileIngestedEvent(FileIngestedEvent event) {
        log.info("=== FILE INGESTED EVENT ===");
        log.info("Batch ID: {}", event.getBatchId());
        log.info("File Type: {}", event.getFileType());
        log.info("Row Count: {}", event.getRowCount());
        log.info("Timeframe: {}", event.getTimeframe());
        log.info("File Name: {}", event.getFileName());
        log.info("File Size: {} bytes", event.getFileSize());
        log.info("Ingested At: {}", event.getIngestedAt());
        log.info("==========================");
        
        // Here you can add additional processing like:
        // - Sending notifications
        // - Updating dashboards
        // - Triggering data processing workflows
        // - Storing event in audit log
    }
}

