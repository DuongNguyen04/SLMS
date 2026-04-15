package com.example.slms.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;

import com.example.slms.dto.request.ProductCreateRequest;
import com.example.slms.dto.request.ProductUpdateRequest;
import com.example.slms.dto.response.ProductResponse;

public interface ProductService {

	Page<ProductResponse> listProducts(int page, int size, String keyword, BigDecimal minPrice, BigDecimal maxPrice);

	ProductResponse getProductByName(String name);

	ProductResponse createProduct(ProductCreateRequest request);

	ProductResponse updateProduct(String name, ProductUpdateRequest request);

	void deleteProduct(String name);
}
