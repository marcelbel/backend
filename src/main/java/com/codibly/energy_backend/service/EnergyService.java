package com.codibly.energy_backend.service;

import com.codibly.energy_backend.client.CarbonIntensityClient;
import com.codibly.energy_backend.dto.CarbonApiResponse;
import com.codibly.energy_backend.dto.EnergyMixResponse;
import com.codibly.energy_backend.dto.OptimalChargingWindowResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnergyService {

    private static final Set<String> CLEAN_FUELS = Set.of("biomass", "nuclear", "hydro", "wind", "solar");

    private final CarbonIntensityClient carbonIntensityClient;
    private final Clock clock;

    @Autowired
    public EnergyService(CarbonIntensityClient carbonIntensityClient) {
        this(carbonIntensityClient, Clock.systemUTC());
    }

    EnergyService(CarbonIntensityClient carbonIntensityClient, Clock clock) {
        this.carbonIntensityClient = carbonIntensityClient;
        this.clock = clock;
    }

    public List<EnergyMixResponse> getEnergyMixForThreeDays() {
        LocalDate today = LocalDate.now(clock);
        OffsetDateTime from = startOfDay(today);
        OffsetDateTime to = startOfDay(today.plusDays(3));
        List<CarbonApiResponse.GenerationData> data = getGenerationData(from, to);

        return today.datesUntil(today.plusDays(3))
                .map(date -> toEnergyMixResponse(date, data))
                .toList();
    }

    public OptimalChargingWindowResponse findOptimalChargingWindow(int hours) {
        if (hours < 1 || hours > 6) {
            throw new IllegalArgumentException("Charging window must be between 1 and 6 hours.");
        }
        int intervalsInWindow = hours * 2;
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        OffsetDateTime from = startOfDay(tomorrow);
        OffsetDateTime to = startOfDay(tomorrow.plusDays(2));
        List<CarbonApiResponse.GenerationData> data = getGenerationData(from, to).stream()
                .sorted(Comparator.comparing(CarbonApiResponse.GenerationData::from))
                .toList();

        if (data.size() < intervalsInWindow) {
            throw new IllegalStateException("Not enough generation mix intervals returned by Carbon Intensity API.");
        }

        WindowCandidate best = null;
        for (int startIndex = 0; startIndex <= data.size() - intervalsInWindow; startIndex++) {
            List<CarbonApiResponse.GenerationData> window = data.subList(startIndex, startIndex + intervalsInWindow);
            if (!isContinuous(window)) {
                continue;
            }

            double cleanEnergyPercentage = round(averageCleanEnergy(window));
            WindowCandidate candidate = new WindowCandidate(
                    window.getFirst().from(),
                    window.getLast().to(),
                    cleanEnergyPercentage
            );

            if (best == null || candidate.cleanEnergyPercentage() > best.cleanEnergyPercentage()) {
                best = candidate;
            }
        }

        if (best == null) {
            throw new IllegalStateException("No continuous charging window found in Carbon Intensity API data.");
        }

        return new OptimalChargingWindowResponse(best.start(), best.end(), best.cleanEnergyPercentage());
    }

    private EnergyMixResponse toEnergyMixResponse(LocalDate date, List<CarbonApiResponse.GenerationData> data) {
        List<CarbonApiResponse.GenerationData> dailyData = data.stream()
                .filter(interval -> interval.from() != null)
                .filter(interval -> interval.from().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().equals(date))
                .toList();

        Map<String, Double> averages = dailyData.stream()
                .flatMap(interval -> safeGenerationMix(interval).stream())
                .filter(mix -> mix.fuel() != null)
                .collect(Collectors.groupingBy(
                        mix -> normalizeFuel(mix.fuel()),
                        LinkedHashMap::new,
                        Collectors.averagingDouble(CarbonApiResponse.GenerationMix::perc)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> round(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return new EnergyMixResponse(date, averages, round(averageCleanEnergy(dailyData)));
    }

    private List<CarbonApiResponse.GenerationData> getGenerationData(OffsetDateTime from, OffsetDateTime to) {
        CarbonApiResponse response = carbonIntensityClient.getGenerationMix(from, to);
        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .filter(Objects::nonNull)
                .filter(interval -> interval.from() != null && interval.to() != null)
                .toList();
    }

    private double averageCleanEnergy(List<CarbonApiResponse.GenerationData> intervals) {
        DoubleSummaryStatistics stats = intervals.stream()
                .mapToDouble(this::cleanEnergyPercentage)
                .summaryStatistics();
        return stats.getCount() == 0 ? 0.0 : stats.getAverage();
    }

    private double cleanEnergyPercentage(CarbonApiResponse.GenerationData interval) {
        return safeGenerationMix(interval).stream()
                .filter(mix -> mix.fuel() != null)
                .filter(mix -> CLEAN_FUELS.contains(normalizeFuel(mix.fuel())))
                .mapToDouble(CarbonApiResponse.GenerationMix::perc)
                .sum();
    }

    private String normalizeFuel(String fuel) {
        return fuel.toLowerCase(Locale.ROOT);
    }

    private boolean isContinuous(List<CarbonApiResponse.GenerationData> window) {
        for (int index = 1; index < window.size(); index++) {
            if (!window.get(index - 1).to().equals(window.get(index).from())) {
                return false;
            }
        }
        return true;
    }

    private List<CarbonApiResponse.GenerationMix> safeGenerationMix(CarbonApiResponse.GenerationData interval) {
        if (interval.generationmix() == null) {
            return List.of();
        }
        return new ArrayList<>(interval.generationmix());
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record WindowCandidate(OffsetDateTime start, OffsetDateTime end, double cleanEnergyPercentage) {}
}
