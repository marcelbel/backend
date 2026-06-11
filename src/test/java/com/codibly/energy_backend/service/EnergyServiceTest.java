package com.codibly.energy_backend.service;

import com.codibly.energy_backend.client.CarbonIntensityClient;
import com.codibly.energy_backend.dto.CarbonApiResponse;
import com.codibly.energy_backend.dto.EnergyMixResponse;
import com.codibly.energy_backend.dto.OptimalChargingWindowResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnergyServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-11T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void returnsDailyAverageGenerationMixAndCleanEnergyPercentage() {
        CarbonIntensityClient client = mock(CarbonIntensityClient.class);
        when(client.getGenerationMix(any(), any())).thenReturn(new CarbonApiResponse(List.of(
                interval("2026-06-11T00:00:00Z", 10.0, 20.0, 30.0),
                interval("2026-06-11T00:30:00Z", 30.0, 10.0, 40.0),
                interval("2026-06-12T00:00:00Z", 50.0, 5.0, 20.0)
        )));

        EnergyService service = new EnergyService(client, CLOCK);

        List<EnergyMixResponse> response = service.getEnergyMixForThreeDays();

        assertThat(response).hasSize(3);
        assertThat(response.get(0).date()).isEqualTo(LocalDate.parse("2026-06-11"));
        assertThat(response.get(0).generationMix())
                .containsEntry("gas", 20.0)
                .containsEntry("nuclear", 15.0)
                .containsEntry("wind", 35.0);
        assertThat(response.get(0).cleanEnergyPercentage()).isEqualTo(50.0);
        assertThat(response.get(1).cleanEnergyPercentage()).isEqualTo(25.0);
        assertThat(response.get(2).generationMix()).isEmpty();
    }

    @Test
    void returnsBestContinuousChargingWindowForNextTwoDays() {
        CarbonIntensityClient client = mock(CarbonIntensityClient.class);
        when(client.getGenerationMix(any(), any())).thenReturn(new CarbonApiResponse(List.of(
                interval("2026-06-12T00:00:00Z", 60.0),
                interval("2026-06-12T00:30:00Z", 55.0),
                interval("2026-06-12T01:00:00Z", 80.0),
                interval("2026-06-12T01:30:00Z", 90.0),
                interval("2026-06-12T02:00:00Z", 20.0)
        )));

        EnergyService service = new EnergyService(client, CLOCK);

        OptimalChargingWindowResponse response = service.findOptimalChargingWindow(1);

        assertThat(response.start()).isEqualTo(OffsetDateTime.parse("2026-06-12T01:00:00Z"));
        assertThat(response.end()).isEqualTo(OffsetDateTime.parse("2026-06-12T02:00:00Z"));
        assertThat(response.cleanEnergyPercentage()).isEqualTo(85.0);
    }

    private CarbonApiResponse.GenerationData interval(String from, double gas, double nuclear, double wind) {
        return new CarbonApiResponse.GenerationData(
                OffsetDateTime.parse(from),
                OffsetDateTime.parse(from).plusMinutes(30),
                List.of(
                        new CarbonApiResponse.GenerationMix("gas", gas),
                        new CarbonApiResponse.GenerationMix("nuclear", nuclear),
                        new CarbonApiResponse.GenerationMix("wind", wind)
                )
        );
    }

    private CarbonApiResponse.GenerationData interval(String from, double cleanEnergyPercentage) {
        List<CarbonApiResponse.GenerationMix> generationMix = new ArrayList<>();
        generationMix.add(new CarbonApiResponse.GenerationMix("gas", 100.0 - cleanEnergyPercentage));
        generationMix.add(new CarbonApiResponse.GenerationMix("wind", cleanEnergyPercentage));

        return new CarbonApiResponse.GenerationData(
                OffsetDateTime.parse(from),
                OffsetDateTime.parse(from).plusMinutes(30),
                generationMix
        );
    }
}
