package org.example.discovery_diplom.stats.service;

import org.example.discovery_diplom.stats.dto.EndpointHitDto;
import org.example.discovery_diplom.stats.model.EndpointHit;
import org.example.discovery_diplom.stats.repository.EndpointHitRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EndpointHitServiceTest {

    @Test
    void saveHitCopiesDtoFieldsAndSavesEntity() {
        EndpointHitRepository repository =
                Mockito.mock(EndpointHitRepository.class);

        EndpointHitService service = new EndpointHitService(repository);

        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 6, 12, 0);
        EndpointHitDto dto = new EndpointHitDto("main-service", "/events/1", "192.168.1.10", timestamp);

        service.saveHit(dto);

        ArgumentCaptor<EndpointHit> captor =
                ArgumentCaptor.forClass(EndpointHit.class);

        Mockito.verify(repository).save(captor.capture());

        EndpointHit savedHit = captor.getValue();

        assertEquals("main-service", savedHit.getApp());
        assertEquals("/events/1", savedHit.getUri());
        assertEquals("192.168.1.10", savedHit.getIp());
        assertEquals(timestamp, savedHit.getTimestamp());
        assertNull(savedHit.getId());
    }
}
