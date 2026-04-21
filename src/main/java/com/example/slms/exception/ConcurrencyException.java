package com.example.slms.exception;

import org.springframework.http.HttpStatus;

public class ConcurrencyException extends BusinessException {

    public ConcurrencyException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}