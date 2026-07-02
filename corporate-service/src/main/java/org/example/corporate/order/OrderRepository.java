package org.example.corporate.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
