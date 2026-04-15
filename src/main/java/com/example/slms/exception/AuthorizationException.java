package com.example.slms.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationException extends BusinessException {

    public AuthorizationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
