package com.codibly.energy_backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CarbonApiResponse(
        List<GenerationData> data
) {
    public record GenerationData(
            OffsetDateTime from,
            OffsetDateTime to,
            List<GenerationMix> generationmix
    ) {}

    public record GenerationMix(
            String fuel,
            double perc
    ) {}
}