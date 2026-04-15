package com.example.slms.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.ProductCreateRequest;
import com.example.slms.dto.request.ProductUpdateRequest;
import com.example.slms.dto.response.ProductResponse;
import com.example.slms.service.ProductService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@Validated
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@GetMapping
	@PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
	public ResponseEntity<Page<ProductResponse>> listProducts(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) @DecimalMin(value = "0.0") BigDecimal minPrice,
			@RequestParam(required = false) @DecimalMin(value = "0.0") BigDecimal maxPrice) {
		Page<ProductResponse> response = productService.listProducts(page, size, keyword, minPrice, maxPrice);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{name}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
	public ResponseEntity<ProductResponse> getProduct(@PathVariable @NotBlank String name) {
		ProductResponse response = productService.getProductByName(name);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
		ProductResponse response = productService.createProduct(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{name}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ProductResponse> updateProduct(
			@PathVariable @NotBlank String name,
			@Valid @RequestBody ProductUpdateRequest request) {
		ProductResponse response = productService.updateProduct(name, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{name}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable @NotBlank String name) {
		productService.deleteProduct(name);
		return ResponseEntity.ok(Map.of("name", name, "deleted", true));
	}
}
