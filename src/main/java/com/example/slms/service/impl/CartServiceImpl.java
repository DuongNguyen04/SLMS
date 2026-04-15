package com.example.slms.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.CartItemAddRequest;
import com.example.slms.dto.request.CartItemUpdateRequest;
import com.example.slms.dto.response.CartResponse;
import com.example.slms.entity.Cart;
import com.example.slms.entity.CartItem;
import com.example.slms.entity.Product;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.CartMapper;
import com.example.slms.repository.CartItemRepository;
import com.example.slms.repository.CartRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.CartService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductRepository productRepository;
	private final CartMapper cartMapper;

	@Override
	@Transactional(readOnly = true)
	public CartResponse getMyCart(String customerUsername) {
		Cart cart = cartRepository.findByCustomerUsername(customerUsername)
				.orElseThrow(() -> new BusinessException("Cart not found", HttpStatus.NOT_FOUND));
		return cartMapper.toResponse(cart);
	}

	@Override
	@Transactional
	public CartResponse addItem(String customerUsername, CartItemAddRequest request) {
		String productName = normalizeProductName(request.getProductName());
		validateQuantity(request.getQuantity());

		Product product = productRepository.findByNameIgnoreCase(productName)
				.orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));

		if (product.getStockQuantity() <= 0 || request.getQuantity() > product.getStockQuantity()) {
			throw new ValidationException("Cannot add item with quantity greater than available stock");
		}

		Cart cart = getOrCreateCart(customerUsername);
		CartItem cartItem = cartItemRepository.findByCartAndProduct_NameIgnoreCase(cart, productName)
				.orElseGet(() -> CartItem.builder()
						.cart(cart)
						.product(product)
						.quantity(0)
						.build());

		int updatedQuantity = cartItem.getQuantity() + request.getQuantity();
		if (updatedQuantity > product.getStockQuantity()) {
			throw new ValidationException("Cannot add item with quantity greater than available stock");
		}

		cartItem.setQuantity(updatedQuantity);
		cartItemRepository.save(cartItem);

		return cartMapper.toResponse(cart);
	}

	@Override
	@Transactional
	public CartResponse updateItem(String customerUsername, String productName, CartItemUpdateRequest request) {
		validateQuantity(request.getQuantity());
		String normalizedProductName = normalizeProductName(productName);

		Cart cart = cartRepository.findByCustomerUsername(customerUsername)
				.orElseThrow(() -> new BusinessException("Cart not found", HttpStatus.NOT_FOUND));

		CartItem cartItem = cartItemRepository.findByCartAndProduct_NameIgnoreCase(cart, normalizedProductName)
				.orElseThrow(() -> new BusinessException("Cart item not found", HttpStatus.NOT_FOUND));

		Product product = cartItem.getProduct();
		if (request.getQuantity() > product.getStockQuantity()) {
			throw new ValidationException("Quantity exceeds available stock");
		}

		cartItem.setQuantity(request.getQuantity());
		cartItemRepository.save(cartItem);
		return cartMapper.toResponse(cart);
	}

	@Override
	@Transactional
	public void removeItem(String customerUsername, String productName) {
		String normalizedProductName = normalizeProductName(productName);
		Cart cart = cartRepository.findByCustomerUsername(customerUsername)
				.orElseThrow(() -> new BusinessException("Cart not found", HttpStatus.NOT_FOUND));

		CartItem cartItem = cartItemRepository.findByCartAndProduct_NameIgnoreCase(cart, normalizedProductName)
				.orElseThrow(() -> new BusinessException("Cart item not found", HttpStatus.NOT_FOUND));

		cartItemRepository.delete(cartItem);
	}

	@Override
	@Transactional
	public void clearCart(String customerUsername) {
		Cart cart = cartRepository.findByCustomerUsername(customerUsername)
				.orElseThrow(() -> new BusinessException("Cart not found", HttpStatus.NOT_FOUND));
		cart.getItems().clear();
		cartRepository.save(cart);
	}

	private Cart getOrCreateCart(String customerUsername) {
		return cartRepository.findByCustomerUsername(customerUsername)
				.orElseGet(() -> cartRepository.save(Cart.builder().customerUsername(customerUsername).build()));
	}

	private String normalizeProductName(String productName) {
		if (productName == null || productName.trim().isEmpty()) {
			throw new ValidationException("productName is required");
		}

		return productName.trim();
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new ValidationException("quantity must be greater than 0");
		}
	}
}
