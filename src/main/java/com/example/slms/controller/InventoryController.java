package com.example.slms.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.StockAdjustmentRequest;
import com.example.slms.dto.response.ProductResponse;
import com.example.slms.service.InventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@Validated
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@GetMapping
	@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
	public ResponseEntity<Page<ProductResponse>> listInventory(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size,
			@RequestParam(required = false) String keyword) {
		Page<ProductResponse> response = inventoryService.listInventory(page, size, keyword);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{productName}")
	@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
	public ResponseEntity<ProductResponse> adjustStock(
			@PathVariable @NotBlank String productName,
			@Valid @RequestBody StockAdjustmentRequest request) {
		ProductResponse response = inventoryService.adjustStock(productName, request);
		return ResponseEntity.ok(response);
	}
}
