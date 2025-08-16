package com.ecomanalyser.repository;

import com.ecomanalyser.domain.SkuPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkuPriceRepository extends JpaRepository<SkuPriceEntity, Long> {
    Optional<SkuPriceEntity> findBySku(String sku);
}


