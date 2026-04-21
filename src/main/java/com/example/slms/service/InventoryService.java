package com.example.slms.service;

import org.springframework.data.domain.Page;

import com.example.slms.dto.request.StockAdjustmentRequest;
import com.example.slms.dto.response.ProductResponse;

public interface InventoryService {

	Page<ProductResponse> listInventory(int page, int size, String keyword);

	ProductResponse adjustStock(String productName, StockAdjustmentRequest request);
}
