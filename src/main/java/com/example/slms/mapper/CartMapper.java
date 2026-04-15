package com.example.slms.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.example.slms.dto.response.CartResponse;
import com.example.slms.entity.Cart;
import com.example.slms.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalPrice", expression = "java(calculateTotal(entity.getItems()))")
    CartResponse toResponse(Cart entity);

    @Mapping(target = "productName", source = "product.name")
    CartResponse.CartItemData toItemData(CartItem item);

    default BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
