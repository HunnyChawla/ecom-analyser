package com.ecomanalyser.repository;

import com.ecomanalyser.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByOrderDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<OrderEntity> findByOrderId(String orderId);

    @Query("select o.sku, sum(o.quantity) as qty from OrderEntity o where o.orderDateTime between :start and :end group by o.sku order by qty desc")
    List<Object[]> topOrderedSkus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}


