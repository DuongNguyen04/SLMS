package com.example.slms.mapper;

import com.example.slms.dto.response.OrderResponse;
import com.example.slms.entity.CustomerOrder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    OrderResponse toResponse(CustomerOrder entity);
}
