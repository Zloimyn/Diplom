package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.*;
import org.example.discovery_diplom.model.*;
import org.example.discovery_diplom.repository.CompilationRepository;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.ParticipationRequestRepository;
import org.example.discovery_diplom.stats.client.StatsClient;
import org.example.discovery_diplom.stats.dto.ViewStatsDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final StatsClient statsClient;

    public CompilationService(CompilationRepository compilationRepository, EventRepository eventRepository,
                              ParticipationRequestRepository participationRequestRepository, StatsClient statsClient) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.participationRequestRepository = participationRequestRepository;
        this.statsClient = statsClient;
    }

    public CompilationDto createCompilation(NewCompilationDto newDto) {
        if (newDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тело запроса обязательно");
        }
        validateTitle(newDto.getTitle());

        Set<Event> events = new HashSet<>();

        if (newDto.getEvents() != null && !newDto.getEvents().isEmpty()) {
            List<Event> foundEvents = eventRepository.findAllById(newDto.getEvents());

            if (foundEvents.size() != new HashSet<>(newDto.getEvents()).size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Одно или несколько событий не найдены");
            }

            events.addAll(foundEvents);
        }

        Compilation compilation = new Compilation();
        compilation.setTitle(newDto.getTitle());
        compilation.setPinned(newDto.isPinned());
        compilation.setEvents(events);

        return toDto(compilationRepository.save(compilation));
    }

    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Подборка не найдена"));

        if (updateRequest.getTitle() != null) {
            validateTitle(updateRequest.getTitle());
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updateRequest.getEvents());
            if (events.size() != new HashSet<>(updateRequest.getEvents()).size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Одно или несколько событий не найдены");
            }
            compilation.setEvents(new HashSet<>(events));
        }

        return toDto(compilationRepository.save(compilation));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Название подборки должно содержать от 1 до 50 символов");
        }
    }

    private CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.isPinned());

        List<EventShortDto> eventDtos = new ArrayList<>();
        List<String> eventUris = compilation.getEvents().stream()
                .map(event -> "/events/" + event.getId())
                .toList();
        Map<String, Long> viewsByUri = getViewsByUri(eventUris);

        for (Event event : compilation.getEvents()) {
            EventShortDto eventDto = new EventShortDto();
            eventDto.setId(event.getId());
            eventDto.setTitle(event.getTitle());
            eventDto.setAnnotation(event.getAnnotation());
            eventDto.setEventDate(event.getEventDate());
            eventDto.setPaid(event.isPaid());
            eventDto.setViews(viewsByUri.getOrDefault("/events/" + event.getId(), 0L));
            eventDto.setConfirmedRequests(participationRequestRepository
                    .countByEvent_IdAndStatus(event.getId(), RequestStatus.CONFIRMED));

            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(event.getCategory().getId());
            categoryDto.setName(event.getCategory().getName());
            eventDto.setCategory(categoryDto);

            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(event.getInitiator().getId());
            initiatorDto.setName(event.getInitiator().getName());
            eventDto.setInitiator(initiatorDto);

            eventDtos.add(eventDto);
        }

        dto.setEvents(eventDtos);
        return dto;
    }

    private Map<String, Long> getViewsByUri(List<String> uris) {
        if (uris.isEmpty()) {
            return Map.of();
        }

        List<ViewStatsDto> stats = statsClient.getStats(uris);
        return stats.stream().collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));
    }

    public void deleteCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Подборка не найдена"));

        compilationRepository.delete(compilation);
    }

    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Подборка не найдена"));

        return toDto(compilation);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        if (from < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from должен быть >= 0, size должен быть >= 1");
        }

        List<Compilation> compilations = new ArrayList<>(compilationRepository.findAll());

        if (pinned != null) {
            compilations.removeIf(compilation -> compilation.isPinned() != pinned);
        }

        int start = Math.min(from, compilations.size());
        int end = (int) Math.min((long) from + size, compilations.size());

        List<CompilationDto> result = new ArrayList<>();
        for (Compilation compilation : compilations.subList(start, end)) {
            result.add(toDto(compilation));
        }

        return result;
    }
}
