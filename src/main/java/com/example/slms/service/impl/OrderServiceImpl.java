package com.example.slms.service.impl;

import jakarta.persistence.PessimisticLockException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.OrderStatusUpdateRequest;
import com.example.slms.dto.request.PlaceOrderRequest;
import com.example.slms.dto.response.OrderResponse;
import com.example.slms.entity.Cart;
import com.example.slms.entity.CartItem;
import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.OrderItem;
import com.example.slms.entity.Product;
import com.example.slms.entity.Shipment;
import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.entity.enums.ShipmentStatus;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ConcurrencyException;
import com.example.slms.exception.ValidationException;
import com.example.slms.repository.CartRepository;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private static final String WAREHOUSE_LOCATION = "Warehouse";
	private static final String CONCURRENCY_MESSAGE =
			"Concurrent stock update detected. Please retry the operation.";

	private final CartRepository cartRepository;
	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;

	@Override
	@Transactional
	public OrderResponse placeOrder(String customerUsername, PlaceOrderRequest request) {
		try {
			Cart cart = cartRepository.findByCustomerUsername(customerUsername)
					.orElseThrow(() -> new BusinessException("Cart not found", HttpStatus.NOT_FOUND));

			if (cart.getItems().isEmpty()) {
				throw new ValidationException("Order must contain at least one item");
			}

			CustomerOrder order = CustomerOrder.builder()
					.orderId(generateOrderId())
					.customerUsername(customerUsername)
					.status(OrderStatus.PENDING)
					.totalPrice(BigDecimal.ZERO)
					.build();

			List<OrderItem> orderItems = new ArrayList<>();
			BigDecimal totalPrice = BigDecimal.ZERO;

			for (CartItem cartItem : cart.getItems()) {
				if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
					throw new ValidationException("Invalid cart item quantity for product: "
							+ cartItem.getProduct().getName());
				}

				Product lockedProduct = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
						.orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));

				if (cartItem.getQuantity() > lockedProduct.getStockQuantity()) {
					throw new ValidationException("Insufficient stock for product: " + lockedProduct.getName());
				}

				lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() - cartItem.getQuantity());

				OrderItem orderItem = OrderItem.builder()
						.customerOrder(order)
						.product(lockedProduct)
						.quantity(cartItem.getQuantity())
						.unitPrice(lockedProduct.getPrice())
						.build();
				orderItems.add(orderItem);

				BigDecimal lineTotal = lockedProduct.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
				totalPrice = totalPrice.add(lineTotal);
			}

			order.setItems(orderItems);
			order.setTotalPrice(totalPrice);

			Shipment shipment = Shipment.builder()
					.customerOrder(order)
					.status(ShipmentStatus.CREATED)
					.currentLocation(WAREHOUSE_LOCATION)
					.build();
			order.setShipment(shipment);

			CustomerOrder savedOrder = customerOrderRepository.save(order);

			cart.getItems().clear();
			cartRepository.save(cart);

			return toOrderResponse(savedOrder, true);
		} catch (RuntimeException ex) {
			if (isConcurrencyConflict(ex)) {
				throw new ConcurrencyException(CONCURRENCY_MESSAGE);
			}

			throw ex;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderResponse> listOrders(String requesterUsername, boolean canViewAll, int page, int size, OrderStatus status) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
		Page<CustomerOrder> orders;

		if (canViewAll) {
			orders = status == null
					? customerOrderRepository.findAll(pageable)
					: customerOrderRepository.findByStatus(status, pageable);
		} else {
			orders = status == null
					? customerOrderRepository.findByCustomerUsername(requesterUsername, pageable)
					: customerOrderRepository.findByCustomerUsernameAndStatus(requesterUsername, status, pageable);
		}

		if (orders.isEmpty()) {
			throw new BusinessException("No orders found", HttpStatus.NOT_FOUND);
		}

		return orders.map(order -> toOrderResponse(order, false));
	}

	@Override
	@Transactional(readOnly = true)
	public OrderResponse getOrderDetail(String orderId, String requesterUsername, boolean canViewAll) {
		CustomerOrder order = findOrderByIdOrThrow(orderId);
		validateOrderScope(order, requesterUsername, canViewAll);
		return toOrderResponse(order, true);
	}

	@Override
	@Transactional
	public OrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request) {
		CustomerOrder order = findOrderByIdOrThrow(orderId);
		OrderStatus currentStatus = order.getStatus();
		OrderStatus targetStatus = request.getStatus();

		if (!isValidStatusTransition(currentStatus, targetStatus)) {
			throw new ValidationException("Invalid order status transition: " + currentStatus + " -> " + targetStatus);
		}

		order.setStatus(targetStatus);
		syncShipmentWithOrderStatus(order, targetStatus);
		CustomerOrder updatedOrder = customerOrderRepository.save(order);

		return toOrderResponse(updatedOrder, true);
	}

	@Override
	@Transactional
	public OrderResponse cancelOrder(String orderId, String requesterUsername, boolean isAdmin) {
		try {
			CustomerOrder order = findOrderByIdOrThrow(orderId);

			if (!isAdmin && !order.getCustomerUsername().equals(requesterUsername)) {
				throw new BusinessException("You are not allowed to access this order", HttpStatus.FORBIDDEN);
			}

			if (order.getStatus() != OrderStatus.PENDING) {
				throw new ValidationException("Order can only be cancelled when status is PENDING");
			}

			for (OrderItem item : order.getItems()) {
				Product lockedProduct = productRepository.findByIdForUpdate(item.getProduct().getId())
						.orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));

				lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() + item.getQuantity());
			}

			order.setStatus(OrderStatus.CANCELLED);
			if (order.getShipment() != null) {
				order.getShipment().setCurrentLocation("Order Cancelled");
			}

			CustomerOrder updatedOrder = customerOrderRepository.save(order);
			return toOrderResponse(updatedOrder, true);
		} catch (RuntimeException ex) {
			if (isConcurrencyConflict(ex)) {
				throw new ConcurrencyException(CONCURRENCY_MESSAGE);
			}

			throw ex;
		}
	}

	private CustomerOrder findOrderByIdOrThrow(String orderId) {
		if (orderId == null || orderId.trim().isEmpty()) {
			throw new ValidationException("orderId is required");
		}

		return customerOrderRepository.findByOrderId(orderId.trim())
				.orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
	}

	private void validateOrderScope(CustomerOrder order, String requesterUsername, boolean canViewAll) {
		if (!canViewAll && !order.getCustomerUsername().equals(requesterUsername)) {
			throw new BusinessException("You are not allowed to access this order", HttpStatus.FORBIDDEN);
		}
	}

	private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
		if (targetStatus == null || currentStatus == null || currentStatus == targetStatus) {
			return false;
		}

		return switch (currentStatus) {
			case PENDING -> targetStatus == OrderStatus.CONFIRMED;
			case CONFIRMED -> targetStatus == OrderStatus.SHIPPED;
			case SHIPPED -> targetStatus == OrderStatus.DELIVERED;
			case DELIVERED, CANCELLED -> false;
		};
	}

	private void syncShipmentWithOrderStatus(CustomerOrder order, OrderStatus targetStatus) {
		Shipment shipment = order.getShipment();
		if (shipment == null) {
			return;
		}

		if (targetStatus == OrderStatus.SHIPPED && shipment.getStatus() == ShipmentStatus.CREATED) {
			shipment.setStatus(ShipmentStatus.IN_TRANSIT);
			shipment.setCurrentLocation("In Transit");
		}

		if (targetStatus == OrderStatus.DELIVERED && shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
			shipment.setStatus(ShipmentStatus.DELIVERED);
			shipment.setCurrentLocation("Delivered");
		}
	}

	private String generateOrderId() {
		String orderId;
		do {
			String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
			orderId = "ORD-" + suffix;
		} while (customerOrderRepository.findByOrderId(orderId).isPresent());

		return orderId;
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

	private OrderResponse toOrderResponse(CustomerOrder order, boolean includeDetails) {
		OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
				.orderId(order.getOrderId())
				.customerUsername(order.getCustomerUsername())
				.totalPrice(order.getTotalPrice())
				.status(order.getStatus());

		if (!includeDetails) {
			return builder.build();
		}

		List<OrderResponse.OrderItemData> itemData = order.getItems().stream()
				.map(item -> OrderResponse.OrderItemData.builder()
						.productName(item.getProduct().getName())
						.imageUrl(item.getProduct().getImageUrl())
						.quantity(item.getQuantity())
						.unitPrice(item.getUnitPrice())
						.build())
				.toList();

		builder.items(itemData);

		if (order.getShipment() != null) {
			builder.shipment(OrderResponse.ShipmentData.builder()
					.orderId(order.getOrderId())
					.status(order.getShipment().getStatus())
					.currentLocation(order.getShipment().getCurrentLocation())
					.build());
		}

		return builder.build();
	}
}
