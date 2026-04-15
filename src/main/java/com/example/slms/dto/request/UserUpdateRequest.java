package com.example.slms.dto.request;

import com.example.slms.entity.enums.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    private String password;
    private Role role;
}
