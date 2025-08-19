package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments_raw")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRawEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;
    
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;
    
    @Column(name = "validation_status")
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;
    
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum ValidationStatus {
        PENDING, VALID, INVALID, PROCESSED
    }
}

