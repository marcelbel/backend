package com.codibly.energy_backend.client;

import com.codibly.energy_backend.dto.CarbonApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class CarbonIntensityClient {

    private static final DateTimeFormatter CARBON_API_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

    private final RestClient restClient;

    public CarbonIntensityClient(@Value("${carbon.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public CarbonApiResponse getGenerationMix(OffsetDateTime from, OffsetDateTime to) {
        String fromText = formatDateTime(from);
        String toText = formatDateTime(to);

        return restClient.get()
                .uri("/generation/" + fromText + "/" + toText)
                .retrieve()
                .body(CarbonApiResponse.class);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime
                .withOffsetSameInstant(ZoneOffset.UTC)
                .format(CARBON_API_FORMATTER);
    }
}