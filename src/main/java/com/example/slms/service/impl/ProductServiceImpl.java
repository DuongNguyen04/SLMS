package com.example.slms.service.impl;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.ProductCreateRequest;
import com.example.slms.dto.request.ProductUpdateRequest;
import com.example.slms.dto.response.ProductResponse;
import com.example.slms.entity.Product;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.ProductMapper;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final ProductMapper productMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<ProductResponse> listProducts(int page, int size, String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
		validatePriceRange(minPrice, maxPrice);

		Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
		Specification<Product> specification = buildProductSpecification(keyword, minPrice, maxPrice);

		Page<ProductResponse> products = productRepository.findAll(specification, pageable)
				.map(productMapper::toResponse);

		if (products.isEmpty()) {
			throw new BusinessException("No products found", HttpStatus.NOT_FOUND);
		}

		return products;
	}

	@Override
	@Transactional(readOnly = true)
	public ProductResponse getProductByName(String name) {
		Product product = findByNameOrThrow(name);
		return productMapper.toResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse createProduct(ProductCreateRequest request) {
		String name = normalizeName(request.getName());
		validatePrice(request.getPrice());
		validateStockQuantity(request.getStockQuantity());

		if (productRepository.existsByNameIgnoreCase(name)) {
			throw new ValidationException("Product name already exists");
		}

		Product product = Product.builder()
				.name(name)
				.price(request.getPrice())
				.stockQuantity(request.getStockQuantity())
				.build();

		Product savedProduct = productRepository.save(product);
		return productMapper.toResponse(savedProduct);
	}

	@Override
	@Transactional
	public ProductResponse updateProduct(String name, ProductUpdateRequest request) {
		Product product = findByNameOrThrow(name);

		boolean hasUpdate = false;
		if (request.getName() != null) {
			String updatedName = normalizeName(request.getName());
			boolean nameChanged = !product.getName().equalsIgnoreCase(updatedName);
			if (nameChanged && productRepository.existsByNameIgnoreCase(updatedName)) {
				throw new ValidationException("Product name already exists");
			}

			product.setName(updatedName);
			hasUpdate = true;
		}

		if (request.getPrice() != null) {
			validatePrice(request.getPrice());
			product.setPrice(request.getPrice());
			hasUpdate = true;
		}

		if (request.getStockQuantity() != null) {
			validateStockQuantity(request.getStockQuantity());
			product.setStockQuantity(request.getStockQuantity());
			hasUpdate = true;
		}

		if (!hasUpdate) {
			throw new ValidationException("At least one field (name, price, stockQuantity) must be provided");
		}

		Product updatedProduct = productRepository.save(product);
		return productMapper.toResponse(updatedProduct);
	}

	@Override
	@Transactional
	public void deleteProduct(String name) {
		Product product = findByNameOrThrow(name);
		productRepository.delete(product);
	}

	private Product findByNameOrThrow(String name) {
		String normalizedName = normalizeName(name);
		return productRepository.findByNameIgnoreCase(normalizedName)
				.orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));
	}

	private Specification<Product> buildProductSpecification(String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
		return Specification.where(hasKeyword(keyword))
				.and(hasMinPrice(minPrice))
				.and(hasMaxPrice(maxPrice));
	}

	private Specification<Product> hasKeyword(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return null;
		}

		String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
		return (root, query, criteriaBuilder) -> criteriaBuilder.like(
				criteriaBuilder.lower(root.get("name")),
				normalizedKeyword);
	}

	private Specification<Product> hasMinPrice(BigDecimal minPrice) {
		if (minPrice == null) {
			return null;
		}

		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
	}

	private Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
		if (maxPrice == null) {
			return null;
		}

		return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
	}

	private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new ValidationException("minPrice must be greater than or equal to 0");
		}

		if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new ValidationException("maxPrice must be greater than or equal to 0");
		}

		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new ValidationException("minPrice must be less than or equal to maxPrice");
		}
	}

	private void validatePrice(BigDecimal price) {
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ValidationException("price must be greater than 0");
		}
	}

	private void validateStockQuantity(Integer stockQuantity) {
		if (stockQuantity == null || stockQuantity < 0) {
			throw new ValidationException("stockQuantity must be greater than or equal to 0");
		}
	}

	private String normalizeName(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new ValidationException("name is required");
		}

		return name.trim();
	}
}
