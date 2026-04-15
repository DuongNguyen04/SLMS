package com.example.slms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.slms.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
