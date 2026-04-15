package com.example.slms.mapper;

import com.example.slms.dto.response.ProductResponse;
import com.example.slms.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product entity);
}
