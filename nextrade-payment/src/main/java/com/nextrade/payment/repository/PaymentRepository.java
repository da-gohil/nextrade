package com.nextrade.payment.repository;

import com.nextrade.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByIdempotencyKey(String key);
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    boolean existsByIdempotencyKey(String key);
}
