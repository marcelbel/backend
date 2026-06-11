package com.codibly.energy_backend.controller;

import com.codibly.energy_backend.dto.OptimalChargingWindowResponse;
import com.codibly.energy_backend.dto.EnergyMixResponse;
import com.codibly.energy_backend.service.EnergyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class EnergyController {

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/energy")
    public Map<String, String> getAvailableEndpoints() {
        return Map.of(
                "energyMix", "/api/energy/mix",
                "optimalChargingWindow", "/api/energy/optimal-charging-window?hours=1"
        );
    }

    @GetMapping({"/energy-mix", "/energy/mix"})
    public List<EnergyMixResponse> getEnergyMix() {
        return energyService.getEnergyMixForThreeDays();
    }

    @GetMapping({"/charging-window", "/energy/optimal-charging-window"})
    public OptimalChargingWindowResponse getChargingWindow(@RequestParam @Min(1) @Max(6) int hours) {
        return energyService.findOptimalChargingWindow(hours);
    }
}
