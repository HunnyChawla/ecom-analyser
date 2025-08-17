package com.ecomanalyser.repository;

import com.ecomanalyser.domain.NormalizedOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NormalizedOrderRepository extends JpaRepository<NormalizedOrderEntity, Long> {
    
    Optional<NormalizedOrderEntity> findByOrderId(String orderId);
    
    List<NormalizedOrderEntity> findByBatchId(String batchId);
    
    @Query("SELECT COUNT(n) FROM NormalizedOrderEntity n WHERE n.batchId = :batchId")
    Long countByBatchId(@Param("batchId") String batchId);
    
    List<NormalizedOrderEntity> findByStandardizedStatus(String status);
    
    List<NormalizedOrderEntity> findByOrderDateBetween(LocalDate start, LocalDate end);
    
    List<NormalizedOrderEntity> findBySkuResolved(Boolean skuResolved);
    
    @Query("SELECT n FROM NormalizedOrderEntity n WHERE n.standardizedStatus = :status AND n.orderDate BETWEEN :start AND :end")
    List<NormalizedOrderEntity> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
    
    @Query("SELECT COUNT(n) FROM NormalizedOrderEntity n WHERE n.batchId = :batchId AND n.standardizedStatus = :status")
    long countByBatchIdAndStatus(@Param("batchId") String batchId, @Param("status") String status);
    
    @Query("SELECT COUNT(n) FROM NormalizedOrderEntity n WHERE n.batchId = :batchId AND n.skuResolved = :skuResolved")
    long countByBatchIdAndSkuResolved(@Param("batchId") String batchId, @Param("skuResolved") Boolean skuResolved);
    
    @Query("SELECT n.standardizedStatus, COUNT(n) FROM NormalizedOrderEntity n WHERE n.batchId = :batchId GROUP BY n.standardizedStatus")
    List<Object[]> getStatusCountsByBatchId(@Param("batchId") String batchId);
    
    @Query("SELECT n.sku, COUNT(n) FROM NormalizedOrderEntity n WHERE n.batchId = :batchId GROUP BY n.sku ORDER BY COUNT(n) DESC")
    List<Object[]> getTopSkusByBatchId(@Param("batchId") String batchId);
    
    void deleteByBatchId(String batchId);
}
