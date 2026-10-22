package org.example.discovery_diplom.stats.client;

import org.example.discovery_diplom.stats.dto.EndpointHitDto;
import org.example.discovery_diplom.stats.dto.ViewStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatsClient {
    private final RestClient restClient;

    // Создаем RestClient
    public StatsClient(RestClient.Builder builder, @Value("${stats-server.url}") String statsServerUrl) {
        this.restClient = builder
                .baseUrl(statsServerUrl)
                .build();
    }

    // Отправить 1 просмотр в статический сервис
    public void sendHit(EndpointHitDto dto) {
        restClient.post()
                .uri("/hit")
                .body(dto) // Объект в Json
                .retrieve()
                .toBodilessEntity(); // Отправка
    }

    public List<ViewStatsDto> getStats(List<String> uris) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stats")
                        .queryParam("start", LocalDateTime.of(2000, 1, 1, 0, 0))
                        .queryParam("end", LocalDateTime.now())
                        .queryParam("unique", true)
                        .queryParam("uris", uris.toArray())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
