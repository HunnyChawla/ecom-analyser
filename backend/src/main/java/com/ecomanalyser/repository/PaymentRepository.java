package com.ecomanalyser.repository;

import com.ecomanalyser.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByPaymentDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    
    Optional<PaymentEntity> findByOrderId(String orderId);

    // Fixed query: Use order date instead of payment date, and add month information
    @Query("select p.orderStatus, count(distinct p.orderId) as orderCount, " +
           "EXTRACT(MONTH FROM p.paymentDateTime) as month, " +
           "EXTRACT(YEAR FROM p.paymentDateTime) as year " +
           "from PaymentEntity p " +
           "where p.orderStatus is not null " +
           "group by p.orderStatus, EXTRACT(MONTH FROM p.paymentDateTime), EXTRACT(YEAR FROM p.paymentDateTime) " +
           "order by year desc, month desc, orderCount desc")
    List<Object[]> getOrderCountsByStatusWithMonth();
    
    // Filtered by date range
    @Query("select p.orderStatus, count(distinct p.orderId) as orderCount from PaymentEntity p where p.paymentDateTime between :start and :end and p.orderStatus is not null group by p.orderStatus order by orderCount desc")
    List<Object[]> getOrderCountsByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}


