package com.example.slms.service;

import org.springframework.data.domain.Page;

import com.example.slms.dto.request.UserCreateRequest;
import com.example.slms.dto.request.UserUpdateRequest;
import com.example.slms.dto.response.UserResponse;
import com.example.slms.entity.enums.Role;

public interface UserAdminService {

	Page<UserResponse> listUsers(int page, int size);

	UserResponse createUser(UserCreateRequest request);

	UserResponse updateUser(String username, UserUpdateRequest request);

	void deleteUser(String username);

	UserResponse assignRole(String username, Role role);
}
