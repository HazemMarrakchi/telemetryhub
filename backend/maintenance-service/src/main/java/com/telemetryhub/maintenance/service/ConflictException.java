package com.telemetryhub.maintenance.service;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}