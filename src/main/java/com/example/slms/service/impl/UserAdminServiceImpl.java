package com.example.slms.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.UserCreateRequest;
import com.example.slms.dto.request.UserUpdateRequest;
import com.example.slms.dto.response.UserResponse;
import com.example.slms.entity.UserAccount;
import com.example.slms.entity.enums.Role;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.UserAccountMapper;
import com.example.slms.repository.UserAccountRepository;
import com.example.slms.service.UserAdminService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

	private final UserAccountRepository userAccountRepository;
	private final UserAccountMapper userAccountMapper;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true)
	public Page<UserResponse> listUsers(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
		Page<UserResponse> users = userAccountRepository.findAll(pageable)
				.map(userAccountMapper::toResponse);

		if (users.isEmpty()) {
			throw new BusinessException("No users found", HttpStatus.NOT_FOUND);
		}

		return users;
	}

	@Override
	@Transactional
	public UserResponse createUser(UserCreateRequest request) {
		String username = normalizeUsername(request.getUsername());
		if (userAccountRepository.existsByUsername(username)) {
			throw new ValidationException("Username already exists");
		}

		UserAccount user = UserAccount.builder()
				.username(username)
				.password(passwordEncoder.encode(request.getPassword()))
				.role(request.getRole())
				.build();

		UserAccount savedUser = userAccountRepository.save(user);
		return userAccountMapper.toResponse(savedUser);
	}

	@Override
	@Transactional
	public UserResponse updateUser(String username, UserUpdateRequest request) {
		UserAccount user = findByUsernameOrThrow(username);

		boolean hasUpdate = false;
		if (request.getPassword() != null) {
			String rawPassword = request.getPassword().trim();
			if (rawPassword.isEmpty()) {
				throw new ValidationException("password must not be blank");
			}

			user.setPassword(passwordEncoder.encode(rawPassword));
			hasUpdate = true;
		}

		if (request.getRole() != null) {
			user.setRole(request.getRole());
			hasUpdate = true;
		}

		if (!hasUpdate) {
			throw new ValidationException("At least one field (password or role) must be provided");
		}

		UserAccount updatedUser = userAccountRepository.save(user);
		return userAccountMapper.toResponse(updatedUser);
	}

	@Override
	@Transactional
	public void deleteUser(String username) {
		UserAccount user = findByUsernameOrThrow(username);
		userAccountRepository.delete(user);
	}

	@Override
	@Transactional
	public UserResponse assignRole(String username, Role role) {
		if (role == null) {
			throw new ValidationException("role is required");
		}

		UserAccount user = findByUsernameOrThrow(username);
		user.setRole(role);
		UserAccount updatedUser = userAccountRepository.save(user);
		return userAccountMapper.toResponse(updatedUser);
	}

	private UserAccount findByUsernameOrThrow(String username) {
		String normalizedUsername = normalizeUsername(username);
		return userAccountRepository.findByUsername(normalizedUsername)
				.orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
	}

	private String normalizeUsername(String username) {
		if (username == null || username.trim().isEmpty()) {
			throw new ValidationException("username is required");
		}

		return username.trim();
	}
}
