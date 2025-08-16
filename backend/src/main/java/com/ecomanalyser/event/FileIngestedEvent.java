package com.ecomanalyser.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileIngestedEvent {
    
    private String batchId;
    private FileType fileType;
    private Integer rowCount;
    private LocalDateTime timeframe;
    private String fileName;
    private Long fileSize;
    private LocalDateTime ingestedAt;
    
    public enum FileType {
        ORDERS, PAYMENTS
    }
}
