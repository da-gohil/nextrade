package com.nextrade.inventory.repository;

import com.nextrade.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByOrderId(Long orderId);

    @Query("SELECT r FROM StockReservation r WHERE r.status = 'RESERVED' AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);
}
