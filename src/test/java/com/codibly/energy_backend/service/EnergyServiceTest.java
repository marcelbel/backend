package com.codibly.energy_backend.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-06-11T00:00:00Z"),
            ZoneOffset.UTC
    );

    private final EnergyService energyService = new EnergyService(null, fixedClock);

    @Test
    void shouldThrowExceptionWhenChargingWindowIsLessThanOneHour() {
        assertThrows(
                IllegalArgumentException.class,
                () -> energyService.findOptimalChargingWindow(0)
        );
    }

    @Test
    void shouldThrowExceptionWhenChargingWindowIsMoreThanSixHours() {
        assertThrows(
                IllegalArgumentException.class,
                () -> energyService.findOptimalChargingWindow(7)
        );
    }
}