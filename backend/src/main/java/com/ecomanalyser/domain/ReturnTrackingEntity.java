package com.ecomanalyser.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_tracking")
public class ReturnTrackingEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;
    
    @Column(name = "sku_id")
    private String skuId;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "return_amount", precision = 10, scale = 2)
    private java.math.BigDecimal returnAmount;
    
    @Column(name = "order_status")
    private String orderStatus;
    
    @Column(name = "order_date")
    private java.time.LocalDate orderDate;
    
    @Column(name = "return_status")
    @Enumerated(EnumType.STRING)
    private ReturnStatus returnStatus;
    
    @Column(name = "received_date")
    private LocalDateTime receivedDate;
    
    @Column(name = "received_by")
    private String receivedBy;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ReturnStatus {
        PENDING_RECEIPT,    // Order is in RETURN/RTO status, waiting to be received
        RECEIVED,           // Order has been physically received
        NOT_RECEIVED        // Order was not received (lost, damaged, etc.)
    }
    
    // Default constructor
    public ReturnTrackingEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.returnStatus = ReturnStatus.PENDING_RECEIPT;
    }
    
    // Constructor with order details
    public ReturnTrackingEntity(String orderId, String skuId, Integer quantity, 
                               java.math.BigDecimal returnAmount, String orderStatus, 
                               java.time.LocalDate orderDate) {
        this();
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.returnAmount = returnAmount;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getSkuId() {
        return skuId;
    }
    
    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public java.math.BigDecimal getReturnAmount() {
        return returnAmount;
    }
    
    public void setReturnAmount(java.math.BigDecimal returnAmount) {
        this.returnAmount = returnAmount;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public java.time.LocalDate getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(java.time.LocalDate orderDate) {
        this.orderDate = orderDate;
    }
    
    public ReturnStatus getReturnStatus() {
        return returnStatus;
    }
    
    public void setReturnStatus(ReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }
    
    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getReceivedBy() {
        return receivedBy;
    }
    
    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods
    public void markAsReceived(String receivedBy, String notes) {
        this.returnStatus = ReturnStatus.RECEIVED;
        this.receivedDate = LocalDateTime.now();
        this.receivedBy = receivedBy;
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsNotReceived(String notes) {
        this.returnStatus = ReturnStatus.NOT_RECEIVED;
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isPendingReceipt() {
        return ReturnStatus.PENDING_RECEIPT.equals(this.returnStatus);
    }
    
    public boolean isReceived() {
        return ReturnStatus.RECEIVED.equals(this.returnStatus);
    }
    
    public boolean isNotReceived() {
        return ReturnStatus.NOT_RECEIVED.equals(this.returnStatus);
    }
}
