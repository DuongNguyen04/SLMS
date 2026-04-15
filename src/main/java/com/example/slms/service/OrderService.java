package com.example.slms.service;

import org.springframework.data.domain.Page;

import com.example.slms.dto.request.OrderStatusUpdateRequest;
import com.example.slms.dto.request.PlaceOrderRequest;
import com.example.slms.dto.response.OrderResponse;
import com.example.slms.entity.enums.OrderStatus;

public interface OrderService {

	OrderResponse placeOrder(String customerUsername, PlaceOrderRequest request);

	Page<OrderResponse> listOrders(String requesterUsername, boolean canViewAll, int page, int size, OrderStatus status);

	OrderResponse getOrderDetail(String orderId, String requesterUsername, boolean canViewAll);

	OrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request);

	OrderResponse cancelOrder(String orderId, String requesterUsername, boolean isAdmin);
}
