package com.codibly.energy_backend.dto;

import java.time.LocalDate;
import java.util.Map;

public record EnergyMixResponse(
        LocalDate date,
        Map<String, Double> generationMix,
        double cleanEnergyPercentage
) {}
