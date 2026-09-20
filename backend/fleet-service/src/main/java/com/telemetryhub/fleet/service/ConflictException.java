package com.telemetryhub.fleet.service;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}