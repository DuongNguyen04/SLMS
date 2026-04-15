package com.example.slms.dto.request;

import com.example.slms.entity.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequest {

    @NotNull(message = "status is required")
    private OrderStatus status;
}
