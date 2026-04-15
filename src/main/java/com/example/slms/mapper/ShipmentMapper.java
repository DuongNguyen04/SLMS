package com.example.slms.mapper;

import com.example.slms.dto.response.ShipmentResponse;
import com.example.slms.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    @Mapping(target = "orderId", source = "customerOrder.orderId")
    ShipmentResponse toResponse(Shipment entity);
}
