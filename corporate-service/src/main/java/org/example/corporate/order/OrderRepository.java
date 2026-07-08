package org.example.corporate.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<OrderEntity> findByPaymentIntentId(String paymentIntentId);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumberSeq();
}
