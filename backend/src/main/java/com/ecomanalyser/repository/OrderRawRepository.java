package com.ecomanalyser.repository;

import com.ecomanalyser.domain.OrderRawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRawRepository extends JpaRepository<OrderRawEntity, Long> {
    
    List<OrderRawEntity> findByBatchId(String batchId);
    
    @Query("SELECT COUNT(o) FROM OrderRawEntity o WHERE o.batchId = :batchId AND o.validationStatus = 'VALID'")
    Long countValidRowsByBatchId(@Param("batchId") String batchId);
    
    @Query("SELECT COUNT(o) FROM OrderRawEntity o WHERE o.batchId = :batchId AND o.validationStatus = 'INVALID'")
    Long countInvalidRowsByBatchId(@Param("batchId") String batchId);
    
    @Query("SELECT COUNT(o) FROM OrderRawEntity o WHERE o.batchId = :batchId")
    Long countTotalRowsByBatchId(@Param("batchId") String batchId);
    
    @Query("SELECT COUNT(o) FROM OrderRawEntity o WHERE o.batchId = :batchId AND o.processed = :processed")
    Long countByBatchIdAndProcessed(@Param("batchId") String batchId, @Param("processed") Boolean processed);
    
    @Query("SELECT o FROM OrderRawEntity o WHERE o.batchId = :batchId AND o.validationStatus = :validationStatus")
    List<OrderRawEntity> findByBatchIdAndValidationStatus(@Param("batchId") String batchId, @Param("validationStatus") OrderRawEntity.ValidationStatus validationStatus);
    
    void deleteByBatchId(String batchId);
}
