package com.example.slms.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.config.JwtTokenProvider;
import com.example.slms.dto.request.LoginRequest;
import com.example.slms.dto.request.RegisterRequest;
import com.example.slms.dto.response.AuthResponse;
import com.example.slms.dto.response.UserResponse;
import com.example.slms.entity.enums.Role;
import com.example.slms.entity.UserAccount;
import com.example.slms.exception.AuthorizationException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.UserAccountMapper;
import com.example.slms.repository.UserAccountRepository;
import com.example.slms.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountMapper userAccountMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userAccountRepository.existsByUsername(username)) {
            throw new ValidationException("Username already exists");
        }

        UserAccount user = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        UserAccount savedUser = userAccountRepository.save(user);
        return userAccountMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new AuthorizationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthorizationException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
