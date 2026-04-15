package com.example.slms.mapper;

import com.example.slms.dto.response.UserResponse;
import com.example.slms.entity.UserAccount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserAccountMapper {

    UserResponse toResponse(UserAccount entity);
}
