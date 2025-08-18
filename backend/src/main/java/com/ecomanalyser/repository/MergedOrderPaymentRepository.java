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
    List<Object[]> findTopOrderedSkus(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("limit") int limit);
    
    @Query("SELECT m.skuId, SUM(m.settlementAmount) as totalProfit FROM MergedOrderPaymentEntity m WHERE m.orderDate BETWEEN :start AND :end AND m.settlementAmount > 0 GROUP BY m.skuId ORDER BY totalProfit DESC")
    List<Object[]> findTopProfitableSkus(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("limit") int limit);
}


