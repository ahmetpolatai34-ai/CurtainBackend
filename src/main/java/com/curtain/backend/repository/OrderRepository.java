package com.curtain.backend.repository;

import com.curtain.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    java.util.List<Order> findByDateBetween(LocalDate startDate, LocalDate endDate);

    java.util.List<Order> findByDateGreaterThanEqual(LocalDate startDate);

    java.util.List<Order> findByDateLessThanEqual(LocalDate endDate);
}
