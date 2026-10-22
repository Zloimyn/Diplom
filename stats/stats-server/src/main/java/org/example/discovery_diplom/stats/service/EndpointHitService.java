package org.example.discovery_diplom.stats.service;

import org.example.discovery_diplom.stats.repository.EndpointHitRepository;
import org.example.discovery_diplom.stats.dto.EndpointHitDto;
import org.example.discovery_diplom.stats.model.EndpointHit;
import org.example.discovery_diplom.stats.dto.ViewStatsDto;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EndpointHitService {
    private final EndpointHitRepository repository;

    public EndpointHitService(EndpointHitRepository repository) {
        this.repository = repository;
    }

    public void saveHit(EndpointHitDto dto) {
        EndpointHit hit = new EndpointHit();

        hit.setApp(dto.getApp());
        hit.setIp(dto.getIp());
        hit.setTimestamp(dto.getTimestamp());
        hit.setUri(dto.getUri());

        repository.save(hit);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris) {
        List<ViewStatsDto> result;

        if (unique) {
            result = repository.findUniqueStats(start, end);
        } else {
            result = repository.findStats(start, end);
        }

        if (uris == null || uris.isEmpty()) {
            return result;
        }

        // Postman-коллекция иногда передаёт несколько uris одним параметром:
        // "/events/1&uris=/events/2". Разбиваем такое значение так же,
        // как если бы параметры uris пришли отдельно.
        Set<String> requestedUris = new HashSet<>();
        for (String uri : uris) {
            for (String requestedUri : uri.split("&uris=")) {
                requestedUris.add(requestedUri);
            }
        }

        return result.stream()
                .filter(stat -> requestedUris.contains(stat.getUri()))
                .toList();
    }
}
