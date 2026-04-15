package com.example.slms.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.OrderStatusUpdateRequest;
import com.example.slms.dto.request.PlaceOrderRequest;
import com.example.slms.dto.response.OrderResponse;
import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@Validated
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<OrderResponse> placeOrder(
			Authentication authentication,
			@RequestBody(required = false) PlaceOrderRequest request) {
		PlaceOrderRequest payload = request == null ? new PlaceOrderRequest() : request;
		OrderResponse response = orderService.placeOrder(authentication.getName(), payload);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
	public ResponseEntity<Page<OrderResponse>> listOrders(
			Authentication authentication,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size,
			@RequestParam(required = false) OrderStatus status) {
		boolean canViewAll = hasAnyRole(authentication, "STAFF", "ADMIN");
		Page<OrderResponse> response = orderService.listOrders(authentication.getName(), canViewAll, page, size, status);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{orderId}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
	public ResponseEntity<OrderResponse> getOrderDetail(
			Authentication authentication,
			@PathVariable @NotBlank String orderId) {
		boolean canViewAll = hasAnyRole(authentication, "STAFF", "ADMIN");
		OrderResponse response = orderService.getOrderDetail(orderId, authentication.getName(), canViewAll);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{orderId}/status")
	@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
	public ResponseEntity<OrderResponse> updateOrderStatus(
			@PathVariable @NotBlank String orderId,
			@Valid @RequestBody OrderStatusUpdateRequest request) {
		OrderResponse response = orderService.updateOrderStatus(orderId, request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{orderId}/cancel")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
	public ResponseEntity<OrderResponse> cancelOrder(
			Authentication authentication,
			@PathVariable @NotBlank String orderId) {
		boolean isAdmin = hasAnyRole(authentication, "ADMIN");
		OrderResponse response = orderService.cancelOrder(orderId, authentication.getName(), isAdmin);
		return ResponseEntity.ok(response);
	}

	private boolean hasAnyRole(Authentication authentication, String... roles) {
		for (String role : roles) {
			String expectedAuthority = "ROLE_" + role;
			boolean matched = authentication.getAuthorities().stream()
					.anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));
			if (matched) {
				return true;
			}
		}

		return false;
	}
}
