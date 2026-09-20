package com.telemetryhub.auth.service;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}