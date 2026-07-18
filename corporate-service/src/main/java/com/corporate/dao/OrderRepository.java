package com.corporate.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import com.corporate.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    Optional<OrderEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<OrderEntity> findByPaymentIntentId(String paymentIntentId);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumberSeq();

    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    long nextInvoiceNumberSeq();
}
