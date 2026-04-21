package com.example.slms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.ShipmentUpdateRequest;
import com.example.slms.dto.response.ShipmentResponse;
import com.example.slms.service.ShipmentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shipments")
@Validated
@RequiredArgsConstructor
public class ShipmentController {

	private final ShipmentService shipmentService;

	@GetMapping("/{orderId}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
	public ResponseEntity<ShipmentResponse> getShipment(
			Authentication authentication,
			@PathVariable @NotBlank String orderId) {
		boolean canViewAll = hasAnyRole(authentication, "STAFF", "ADMIN");
		ShipmentResponse response = shipmentService.getShipment(orderId, authentication.getName(), canViewAll);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{orderId}")
	@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
	public ResponseEntity<ShipmentResponse> updateShipment(
			@PathVariable @NotBlank String orderId,
			@Valid @RequestBody ShipmentUpdateRequest request) {
		ShipmentResponse response = shipmentService.updateShipment(orderId, request);
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
