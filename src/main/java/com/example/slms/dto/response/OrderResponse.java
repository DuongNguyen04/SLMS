package com.example.slms.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.entity.enums.ShipmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;
    private String customerUsername;
    private String shippingAddress;
    private String phoneNumber;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private List<OrderItemData> items;
    private ShipmentData shipment;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private String productName;
        private String imageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentData {
        private String orderId;
        private ShipmentStatus status;
        private String currentLocation;
    }
}
