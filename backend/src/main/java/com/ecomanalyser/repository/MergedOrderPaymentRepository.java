package com.ecomanalyser.repository;

import com.ecomanalyser.domain.MergedOrderPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MergedOrderPaymentRepository extends JpaRepository<MergedOrderPaymentEntity, Long> {
    Optional<MergedOrderPaymentEntity> findByOrderId(String orderId);
}


