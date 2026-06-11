package com.codibly.energy_backend.controller;

import com.codibly.energy_backend.dto.EnergyMixResponse;
import com.codibly.energy_backend.dto.OptimalChargingWindowResponse;
import com.codibly.energy_backend.service.EnergyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class EnergyController {

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/energy-mix")
    public List<EnergyMixResponse> getEnergyMix() {
        return energyService.getEnergyMixForThreeDays();
    }

    @GetMapping("/charging-window")
    public OptimalChargingWindowResponse getChargingWindow(@RequestParam int hours) {
        if (hours < 1 || hours > 6) {
            throw new IllegalArgumentException("Hours must be between 1 and 6.");
        }

        return energyService.findOptimalChargingWindow(hours);
    }
}