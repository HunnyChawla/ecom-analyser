package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merged_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergedOrderPaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. Order id (unique key)
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    // 2. Order Amount (gross): sellingPrice * quantity
    @Column(name = "order_amount")
    private BigDecimal orderAmount;

    // 3. Settlement Amount (bank settlement/final)
    @Column(name = "settlement_amount")
    private BigDecimal settlementAmount;

    // 4. Order Status (resolved)
    @Column(name = "order_status")
    private String orderStatus;

    // 5. SKU ID
    @Column(name = "sku_id")
    private String skuId;

    // 6. Order Date (date only)
    @Column(name = "order_date")
    private LocalDate orderDate;

    // 7. Payment Date (date only)
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    // 8. quantity
    @Column(name = "quantity")
    private Integer quantity;

    // 9. State
    @Column(name = "state")
    private String state;

    // 10. Transaction Id
    @Column(name = "transaction_id")
    private String transactionId;

    // 11. Dispatch Date
    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    // 12. Price Type
    @Column(name = "price_type")
    private String priceType;
}


