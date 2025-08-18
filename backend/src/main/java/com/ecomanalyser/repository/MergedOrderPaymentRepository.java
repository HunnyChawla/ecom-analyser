package com.ecomanalyser.repository;

import com.ecomanalyser.domain.MergedOrderPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MergedOrderPaymentRepository extends JpaRepository<MergedOrderPaymentEntity, String> {
    
    @Query("SELECT m FROM MergedOrderPaymentEntity m WHERE m.orderDate BETWEEN :start AND :end")
    List<MergedOrderPaymentEntity> findByOrderDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT m FROM MergedOrderPaymentEntity m WHERE m.paymentDate BETWEEN :start AND :end")
    List<MergedOrderPaymentEntity> findByPaymentDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT m.orderStatus, COUNT(m) as count FROM MergedOrderPaymentEntity m WHERE m.orderDate BETWEEN :start AND :end GROUP BY m.orderStatus")
    List<Object[]> findOrderStatusCounts(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT m.skuId, SUM(m.quantity) as totalQuantity FROM MergedOrderPaymentEntity m WHERE m.orderDate BETWEEN :start AND :end AND m.skuId IS NOT NULL GROUP BY m.skuId ORDER BY totalQuantity DESC")
    List<Object[]> findTopOrderedSkus(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT m.skuId, SUM(m.settlementAmount) as totalProfit FROM MergedOrderPaymentEntity m WHERE m.orderDate BETWEEN :start AND :end AND m.settlementAmount > 0 GROUP BY m.skuId ORDER BY totalProfit DESC")
    List<Object[]> findTopProfitableSkus(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT m.orderStatus, COUNT(m) as count, EXTRACT(MONTH FROM m.orderDate) as month, EXTRACT(YEAR FROM m.orderDate) as year FROM MergedOrderPaymentEntity m GROUP BY m.orderStatus, EXTRACT(MONTH FROM m.orderDate), EXTRACT(YEAR FROM m.orderDate) ORDER BY year, month, count DESC")
    List<Object[]> findOrderStatusCountsWithMonth();
    
    // Use a working method that we know works
    @Query("SELECT m FROM MergedOrderPaymentEntity m WHERE m.orderStatus = :statusParam")
    List<MergedOrderPaymentEntity> findByOrderStatus(@Param("statusParam") String status);
    
    // Get all data from merged_orders table
    @Query("SELECT m FROM MergedOrderPaymentEntity m ORDER BY m.orderDate DESC")
    List<MergedOrderPaymentEntity> findAllFromMergedOrders();
    
    // Find return orders for tracking (RETURN and RTO statuses)
    @Query("SELECT m.orderId, m.skuId, m.quantity, ABS(m.settlementAmount) as returnAmount, m.orderStatus, m.orderDate " +
           "FROM MergedOrderPaymentEntity m " +
           "WHERE m.orderStatus IN ('RETURN', 'RTO') " +
           "AND m.settlementAmount < 0 " +
           "ORDER BY m.orderDate DESC")
    List<Object[]> findReturnOrdersForTracking();
}


