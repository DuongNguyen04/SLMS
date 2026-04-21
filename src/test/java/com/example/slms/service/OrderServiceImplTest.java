package com.example.slms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.slms.dto.request.OrderStatusUpdateRequest;
import com.example.slms.dto.response.OrderResponse;
import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.exception.ValidationException;
import com.example.slms.repository.CartRepository;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.ProductRepository;
import com.example.slms.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void updateStatusShouldRejectInvalidTransition() {
        CustomerOrder order = CustomerOrder.builder()
                .orderId("ORD-001")
                .customerUsername("alice")
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .build();

        when(customerOrderRepository.findByOrderId("ORD-001")).thenReturn(Optional.of(order));

        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.SHIPPED);

        assertThrows(ValidationException.class, () -> orderService.updateOrderStatus("ORD-001", request));
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void updateStatusShouldAcceptValidTransition() {
        CustomerOrder order = CustomerOrder.builder()
                .orderId("ORD-002")
                .customerUsername("alice")
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .build();

        when(customerOrderRepository.findByOrderId("ORD-002")).thenReturn(Optional.of(order));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        OrderResponse response = orderService.updateOrderStatus("ORD-002", request);

        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(customerOrderRepository).save(any(CustomerOrder.class));
    }
}
