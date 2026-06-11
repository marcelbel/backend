package com.codibly.energy_backend.dto;

import java.time.OffsetDateTime;

public record OptimalChargingWindowResponse(
        OffsetDateTime start,
        OffsetDateTime end,
        double cleanEnergyPercentage
) {}
