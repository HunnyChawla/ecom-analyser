package com.ecomanalyser.repository;

import com.ecomanalyser.domain.NormalizedPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NormalizedPaymentRepository extends JpaRepository<NormalizedPaymentEntity, Long> {
    Optional<NormalizedPaymentEntity> findByOrderId(String orderId);
    long countByBatchId(String batchId);
    void deleteByBatchId(String batchId);
}


