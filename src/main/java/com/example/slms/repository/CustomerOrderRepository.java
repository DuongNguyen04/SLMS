package com.example.slms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.enums.OrderStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByOrderId(String orderId);

    List<CustomerOrder> findByCustomerUsername(String customerUsername);

    Page<CustomerOrder> findByCustomerUsername(String customerUsername, Pageable pageable);

    Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);

    Page<CustomerOrder> findByCustomerUsernameAndStatus(String customerUsername, OrderStatus status, Pageable pageable);
}
