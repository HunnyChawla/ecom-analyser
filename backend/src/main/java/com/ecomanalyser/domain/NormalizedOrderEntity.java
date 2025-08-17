package com.ecomanalyser.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "normalized_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedOrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;
    
    @Column(name = "sku", nullable = false)
    private String sku;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice;
    
    @Column(name = "order_date")
    private LocalDate orderDate;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "customer_state")
    private String customerState;
    
    @Column(name = "size")
    private String size;
    
    @Column(name = "supplier_listed_price", precision = 10, scale = 2)
    private BigDecimal supplierListedPrice;
    
    @Column(name = "supplier_discounted_price", precision = 10, scale = 2)
    private BigDecimal supplierDiscountedPrice;
    
    @Column(name = "packet_id")
    private String packetId;
    
    @Column(name = "standardized_status", nullable = false)
    private String standardizedStatus;
    
    @Column(name = "original_status")
    private String originalStatus;
    
    @Column(name = "supplier_sku")
    private String supplierSku;
    
    @Column(name = "sku_resolved", nullable = false)
    private Boolean skuResolved;
    
    @Column(name = "validation_errors")
    private String validationErrors;
    
    @Column(name = "batch_id")
    private String batchId;
    
    @Column(name = "raw_row_id")
    private Long rawRowId;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
}
