package com.example.slms.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.CartItemAddRequest;
import com.example.slms.dto.request.CartItemUpdateRequest;
import com.example.slms.dto.response.CartResponse;
import com.example.slms.service.CartService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/carts")
@Validated
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<CartResponse> getMyCart(Authentication authentication) {
		CartResponse response = cartService.getMyCart(authentication.getName());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/me/items")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<CartResponse> addItem(Authentication authentication, @Valid @RequestBody CartItemAddRequest request) {
		CartResponse response = cartService.addItem(authentication.getName(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/me/items/{productName}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<CartResponse> updateItem(
			Authentication authentication,
			@PathVariable @NotBlank String productName,
			@Valid @RequestBody CartItemUpdateRequest request) {
		CartResponse response = cartService.updateItem(authentication.getName(), productName, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/me/items/{productName}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<Map<String, Object>> removeItem(
			Authentication authentication,
			@PathVariable @NotBlank String productName) {
		cartService.removeItem(authentication.getName(), productName);
		return ResponseEntity.ok(Map.of(
				"customerUsername", authentication.getName(),
				"productName", productName,
				"deleted", true));
	}

	@DeleteMapping("/me/items")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<Map<String, Object>> clearCart(Authentication authentication) {
		cartService.clearCart(authentication.getName());
		return ResponseEntity.ok(Map.of(
				"customerUsername", authentication.getName(),
				"cleared", true));
	}
}
