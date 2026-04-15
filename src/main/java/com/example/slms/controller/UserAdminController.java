package com.example.slms.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.slms.dto.request.UserCreateRequest;
import com.example.slms.dto.request.UserUpdateRequest;
import com.example.slms.dto.response.UserResponse;
import com.example.slms.service.UserAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@Validated
@RequiredArgsConstructor
public class UserAdminController {

	private final UserAdminService userAdminService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Page<UserResponse>> listUsers(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size) {
		Page<UserResponse> response = userAdminService.listUsers(page, size);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
		UserResponse response = userAdminService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{username}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> updateUser(
			@PathVariable @NotBlank String username,
			@RequestBody UserUpdateRequest request) {
		UserResponse response = userAdminService.updateUser(username, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{username}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable @NotBlank String username) {
		userAdminService.deleteUser(username);
		return ResponseEntity.ok(Map.of("username", username, "deleted", true));
	}

	@PatchMapping("/{username}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> assignRole(
			@PathVariable @NotBlank String username,
			@RequestBody UserUpdateRequest request) {
		UserResponse response = userAdminService.assignRole(username, request.getRole());
		return ResponseEntity.ok(response);
	}
}
