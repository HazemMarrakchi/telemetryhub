package com.telemetryhub.maintenance.api;

public record MonthlyCost(String month, long orders, double totalCost) {
}