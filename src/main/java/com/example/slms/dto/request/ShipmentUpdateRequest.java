package com.example.slms.dto.request;

import com.example.slms.entity.enums.ShipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentUpdateRequest {

    @NotNull(message = "status is required")
    private ShipmentStatus status;

    @NotBlank(message = "currentLocation is required")
    private String currentLocation;
}
