package com.ecomanalyser.repository;

import com.ecomanalyser.domain.ReturnTrackingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnTrackingRepository extends JpaRepository<ReturnTrackingEntity, Long> {
    
    // Find by order ID
    Optional<ReturnTrackingEntity> findByOrderId(String orderId);
    
    // Find by return status
    List<ReturnTrackingEntity> findByReturnStatus(ReturnTrackingEntity.ReturnStatus returnStatus);
    
    // Find by order date range
    List<ReturnTrackingEntity> findByOrderDateBetween(LocalDate start, LocalDate end);
    
    // Find by SKU ID
    List<ReturnTrackingEntity> findBySkuId(String skuId);
    
    // Find by order status (RETURN, RTO, etc.)
    List<ReturnTrackingEntity> findByOrderStatus(String orderStatus);
    
    // Search by order ID containing text
    List<ReturnTrackingEntity> findByOrderIdContainingIgnoreCase(String orderId);
    
    // Search by SKU ID containing text
    List<ReturnTrackingEntity> findBySkuIdContainingIgnoreCase(String skuId);
    
    // Find pending receipts (orders waiting to be received)
    @Query("SELECT r FROM ReturnTrackingEntity r WHERE r.returnStatus = 'PENDING_RECEIPT' ORDER BY r.orderDate DESC")
    List<ReturnTrackingEntity> findPendingReceipts();
    
    // Find received orders
    @Query("SELECT r FROM ReturnTrackingEntity r WHERE r.returnStatus = 'RECEIVED' ORDER BY r.receivedDate DESC")
    List<ReturnTrackingEntity> findReceivedOrders();
    
    // Find not received orders
    @Query("SELECT r FROM ReturnTrackingEntity r WHERE r.returnStatus = 'NOT_RECEIVED' ORDER BY r.updatedAt DESC")
    List<ReturnTrackingEntity> findNotReceivedOrders();
    
    // Advanced search with multiple criteria
    @Query("SELECT r FROM ReturnTrackingEntity r WHERE " +
           "(:orderId = '' OR :orderId IS NULL OR r.orderId LIKE %:orderId%) AND " +
           "(:skuId = '' OR :skuId IS NULL OR r.skuId LIKE %:skuId%) AND " +
           "(:startDate IS NULL OR r.orderDate >= :startDate) AND " +
           "(:endDate IS NULL OR r.orderDate <= :endDate) AND " +
           "(:returnStatus IS NULL OR r.returnStatus = :returnStatus) " +
           "ORDER BY r.orderDate DESC")
    List<ReturnTrackingEntity> searchReturns(
        @Param("orderId") String orderId,
        @Param("skuId") String skuId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("returnStatus") ReturnTrackingEntity.ReturnStatus returnStatus
    );
    
    // Count by return status
    @Query("SELECT CAST(r.returnStatus AS string), COUNT(r) FROM ReturnTrackingEntity r GROUP BY r.returnStatus")
    List<Object[]> countByReturnStatus();
    
    // Check if order exists in return tracking
    boolean existsByOrderId(String orderId);
}
