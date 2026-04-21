package com.example.slms.dto.request;

import com.example.slms.entity.enums.ShipmentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentUpdateRequest {

    private ShipmentStatus status;

    private String currentLocation;
}
