package com.example.slms.dto.response;

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
public class ShipmentResponse {

    private String orderId;
    private ShipmentStatus status;
    private String currentLocation;
}
