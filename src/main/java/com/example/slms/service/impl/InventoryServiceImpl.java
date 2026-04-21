package com.example.slms.service.impl;

import jakarta.persistence.PessimisticLockException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.StockAdjustmentRequest;
import com.example.slms.dto.response.ProductResponse;
import com.example.slms.entity.Product;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ConcurrencyException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.ProductMapper;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.InventoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String CONCURRENCY_MESSAGE =
            "Concurrent stock update detected. Please retry the operation.";

	private final ProductRepository productRepository;
	private final ProductMapper productMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<ProductResponse> listInventory(int page, int size, String keyword) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
		Specification<Product> specification = hasKeyword(keyword);

		Page<ProductResponse> inventory = productRepository.findAll(specification, pageable)
				.map(productMapper::toResponse);

		if (inventory.isEmpty()) {
			throw new BusinessException("No products found", HttpStatus.NOT_FOUND);
		}

		return inventory;
	}

	@Override
	@Transactional
	public ProductResponse adjustStock(String productName, StockAdjustmentRequest request) {
		try {
			Product product = findByNameForUpdateOrThrow(productName);
			Integer stockQuantity = request.getStockQuantity();

			if (stockQuantity == null || stockQuantity < 0) {
				throw new ValidationException("stockQuantity must be greater than or equal to 0");
			}

			product.setStockQuantity(stockQuantity);
			Product updatedProduct = productRepository.save(product);
			return productMapper.toResponse(updatedProduct);
		} catch (RuntimeException ex) {
			if (isConcurrencyConflict(ex)) {
				throw new ConcurrencyException(CONCURRENCY_MESSAGE);
			}

			throw ex;
		}
	}

	private Product findByNameOrThrow(String productName) {
		String normalizedName = normalizeName(productName);
		return productRepository.findByNameIgnoreCase(normalizedName)
				.orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));
	}

	private Product findByNameForUpdateOrThrow(String productName) {
		String normalizedName = normalizeName(productName);
		return productRepository.findByNameIgnoreCaseForUpdate(normalizedName)
				.orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));
	}

	private String normalizeName(String productName) {
		if (productName == null || productName.trim().isEmpty()) {
			throw new ValidationException("productName is required");
		}

		return productName.trim();
	}

	private Specification<Product> hasKeyword(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return null;
		}

		String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
		return (root, query, criteriaBuilder) -> criteriaBuilder.like(
				criteriaBuilder.lower(root.get("name").as(String.class)),
				normalizedKeyword);
	}

	private boolean isConcurrencyConflict(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof CannotAcquireLockException
					|| current instanceof PessimisticLockException
					|| current instanceof PessimisticLockingFailureException) {
				return true;
			}
			current = current.getCause();
		}

		return false;
	}
}
