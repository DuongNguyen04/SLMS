package com.example.slms.service;

import com.example.slms.dto.request.CartItemAddRequest;
import com.example.slms.dto.request.CartItemUpdateRequest;
import com.example.slms.dto.response.CartResponse;

public interface CartService {

	CartResponse getMyCart(String customerUsername);

	CartResponse addItem(String customerUsername, CartItemAddRequest request);

	CartResponse updateItem(String customerUsername, String productName, CartItemUpdateRequest request);

	void removeItem(String customerUsername, String productName);

	void clearCart(String customerUsername);
}
