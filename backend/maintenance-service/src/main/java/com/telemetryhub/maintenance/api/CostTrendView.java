package com.telemetryhub.maintenance.api;

import java.util.List;

public record CostTrendView(List<MonthlyCost> months) {
}