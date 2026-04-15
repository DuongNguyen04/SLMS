package com.example.slms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.slms.entity.Cart;
import com.example.slms.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	Optional<CartItem> findByCartAndProduct_NameIgnoreCase(Cart cart, String productName);

	void deleteByCartAndProduct_NameIgnoreCase(Cart cart, String productName);
}
