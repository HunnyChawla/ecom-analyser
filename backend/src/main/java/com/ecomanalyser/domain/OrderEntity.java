package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "order_date_time", nullable = false)
    private LocalDateTime orderDateTime;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "customer_state")
    private String customerState;

    @Column
    private String size;

    @Column(name = "supplier_listed_price")
    private BigDecimal supplierListedPrice;

    @Column(name = "supplier_discounted_price")
    private BigDecimal supplierDiscountedPrice;

    @Column(name = "packet_id")
    private String packetId;

    @Column(name = "reason_for_credit_entry")
    private String reasonForCreditEntry;
}


