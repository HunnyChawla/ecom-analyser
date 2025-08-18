package com.ecomanalyser.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileIngestedEventListener {

    private final KafkaTemplate<String, FileIngestedEvent> kafkaTemplate;
    
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
        
        // Publish to Kafka for downstream processing/auto-triggering
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send("file.ingested", event.getBatchId(), event);
                log.info("Published file.ingested event to Kafka for batch {}", event.getBatchId());
            } catch (Exception e) {
                log.warn("Failed to publish file.ingested to Kafka: {}", e.getMessage());
            }
        } else {
            log.debug("Kafka disabled; skipping publish of file.ingested");
        }
    }
}

