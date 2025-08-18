package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "normalized_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NormalizedPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "standardized_status", nullable = false)
    private String standardizedStatus;

    @Column(name = "original_status")
    private String originalStatus;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "price_type")
    private String priceType;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "raw_row_id")
    private Long rawRowId;
}


