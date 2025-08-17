package com.ecomanalyser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {
    
    private String batchId;
    private Integer acceptedRows;
    private Integer rejectedRows;
    private Integer warningsCount;
    private LocalDateTime ingestedAt;
    private List<String> warnings;
    private List<String> errors;
}

