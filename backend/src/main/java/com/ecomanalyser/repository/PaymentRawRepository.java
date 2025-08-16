package com.ecomanalyser.repository;

import com.ecomanalyser.domain.PaymentRawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRawRepository extends JpaRepository<PaymentRawEntity, Long> {
    
    List<PaymentRawEntity> findByBatchId(String batchId);
    
    @Query("SELECT COUNT(p) FROM PaymentRawEntity p WHERE p.batchId = :batchId AND p.validationStatus = 'VALID'")
    Long countValidRowsByBatchId(@Param("batchId") String batchId);
    
    @Query("SELECT COUNT(p) FROM PaymentRawEntity p WHERE p.batchId = :batchId AND p.validationStatus = 'INVALID'")
    Long countInvalidRowsByBatchId(@Param("batchId") String batchId);
    
    @Query("SELECT COUNT(p) FROM PaymentRawEntity p WHERE p.batchId = :batchId")
    Long countTotalRowsByBatchId(@Param("batchId") String batchId);
    
    void deleteByBatchId(String batchId);
}
