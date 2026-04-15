package com.example.slms.service;

import com.example.slms.dto.request.LoginRequest;
import com.example.slms.dto.request.RegisterRequest;
import com.example.slms.dto.response.AuthResponse;
import com.example.slms.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
