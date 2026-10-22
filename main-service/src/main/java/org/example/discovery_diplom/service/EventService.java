package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.*;
import org.example.discovery_diplom.exception.CategoryNotFoundException;
import org.example.discovery_diplom.exception.EventConflictException;
import org.example.discovery_diplom.exception.EventNotFoundException;
import org.example.discovery_diplom.exception.UserNotFoundException;
import org.example.discovery_diplom.model.*;
import org.example.discovery_diplom.repository.CategoryRepository;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.ParticipationRequestRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.example.discovery_diplom.stats.client.StatsClient;
import org.example.discovery_diplom.stats.dto.EndpointHitDto;
import org.example.discovery_diplom.stats.dto.ViewStatsDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final StatsClient statsClient;

    public EventService(EventRepository eventRepository, CategoryRepository categoryRepository,
                        UserRepository userRepository, ParticipationRequestRepository participationRequestRepository,
                        StatsClient statsClient) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.participationRequestRepository = participationRequestRepository;
        this.statsClient = statsClient;
    }

    private void registerHit(String uri, String ip) {
        EndpointHitDto hit = new EndpointHitDto(
                "Discovery_diplom",
                uri,
                ip,
                LocalDateTime.now()
        );
        statsClient.sendHit(hit);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с id=" + categoryId + " не найдена"));
    }

    public List<EventShortDto> getPublishedEvents(String text, List<Long> categories,
                                                  Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable, String sort, int from, int size,
                                                  String uri, String ip) {
        validateDateRange(rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAllByState(EventState.PUBLISHED);

        if (categories != null && !categories.isEmpty()) {
            events = events.stream()
                    .filter(event -> categories.contains(event.getCategory().getId()))
                    .collect(Collectors.toList());
        }

        if (paid != null) {
            events = events.stream()
                    .filter(event -> event.isPaid() == paid)
                    .collect(Collectors.toList());
        }

        if (text != null && !text.isBlank()) {
            String searchText = text.toLowerCase(Locale.ROOT);

            events = events.stream()
                    .filter(event ->
                            event.getAnnotation().toLowerCase(Locale.ROOT).contains(searchText)
                                    || event.getDescription().toLowerCase(Locale.ROOT).contains(searchText))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0
                            || getConfirmedRequests(event.getId()) < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        if (rangeStart == null && rangeEnd == null) {
            LocalDateTime now = LocalDateTime.now();
            events = events.stream()
                    .filter(event -> event.getEventDate().isAfter(now))
                    .collect(Collectors.toList());
        } else {
            if (rangeStart != null) {
                events = events.stream()
                        .filter(event -> !event.getEventDate().isBefore(rangeStart))
                        .collect(Collectors.toList());
            }
            if (rangeEnd != null) {
                events = events.stream()
                        .filter(event -> !event.getEventDate().isAfter(rangeEnd))
                        .collect(Collectors.toList());
            }
        }

        if (from < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from должен быть >= 0 size должен быть >= 1");
        }

        Map<String, Long> viewsForSorting = Map.of();
        if ("EVENT_DATE".equals(sort)) {
            events = events.stream()
                    .sorted(Comparator.comparing(Event::getEventDate))
                    .collect(Collectors.toList());
        } else if ("VIEWS".equals(sort)) {
            viewsForSorting = getViewsForEvents(events);
            Map<String, Long> eventViews = viewsForSorting;
            events = events.stream()
                    .sorted(Comparator.comparingLong((Event event) ->
                            eventViews.getOrDefault("/events/" + event.getId(), 0L)).reversed())
                    .collect(Collectors.toList());
        }

        int start = Math.min(from, events.size());
        int end = (int) Math.min((long) from + size, events.size());
        events = new ArrayList<>(events.subList(start, end));

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto eventDto = new EventShortDto();

            eventDto.setId(event.getId());
            eventDto.setTitle(event.getTitle());
            eventDto.setAnnotation(event.getAnnotation());
            eventDto.setEventDate(event.getEventDate());
            eventDto.setPaid(event.isPaid());

            eventDto.setViews(0L);
            eventDto.setConfirmedRequests(getConfirmedRequests(event.getId()));

            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(event.getCategory().getId());
            categoryDto.setName(event.getCategory().getName());
            eventDto.setCategory(categoryDto);

            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(event.getInitiator().getId());
            initiatorDto.setName(event.getInitiator().getName());
            eventDto.setInitiator(initiatorDto);

            result.add(eventDto);
        }

        registerHit(uri, ip);
        Map<String, Long> viewsByUri = viewsForSorting.isEmpty()
                ? getViewsForEvents(events)
                : viewsForSorting;
        for (EventShortDto eventDto : result) {
            eventDto.setViews(viewsByUri.getOrDefault("/events/" + eventDto.getId(), 0L));
        }
        return result;
    }

    public EventFullDto getPublicEvent(Long eventId, String uri, String ip) {
        Event event = findEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new EventNotFoundException("Событие с id=" + eventId + " не найдено");
        }

        registerHit(uri, ip);
        return toEventFullDto(event);
    }

    private Map<String, Long> getViewsForEvents(List<Event> events) {
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();
        return getViewsByUri(uris);
    }

    private Map<String, Long> getViewsByUri(List<String> uris) {
        if (uris.isEmpty()) {
            return Map.of();
        }

        List<ViewStatsDto> stats = statsClient.getStats(uris);
        return stats.stream().collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));
    }

    private long getConfirmedRequests(Long eventId) {
        return participationRequestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        findUserOrThrow(userId);

        Event event = findEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EventNotFoundException("Событие с id=" + eventId + " не найдено");
        }

        return toEventFullDto(event);
    }

    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        validateNewEvent(newEventDto);
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата события должна быть не раньше чем через два часа");
        }

        User initiator = findUserOrThrow(userId);
        Category category = findCategoryOrThrow(newEventDto.getCategory());

        Event event = new Event();
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setAnnotation(newEventDto.getAnnotation());
        event.setTitle(newEventDto.getTitle());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(newEventDto.getEventDate());
        event.setPaid(newEventDto.isPaid());
        event.setParticipantLimit(newEventDto.getParticipantLimit());
        event.setRequestModeration(newEventDto.isRequestModeration());

        Location location = new Location();
        location.setLat(newEventDto.getLocation().getLat());
        location.setLon(newEventDto.getLocation().getLon());
        event.setLocation(location);

        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);

        return toEventFullDto(savedEvent);
    }

    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateDto) {
        findUserOrThrow(userId);

        Event event = findEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EventNotFoundException("Событие с id=" + eventId + " не найдено");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new EventConflictException("Изменять можно только события PENDING или CANCELED");
        }

        validateUpdateEvent(updateDto);
        if (updateDto.getEventDate() != null
                && updateDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата события должна быть не раньше чем через два часа");
        }

        if (updateDto.getAnnotation() != null) {
            event.setAnnotation(updateDto.getAnnotation());
        }
        if (updateDto.getDescription() != null) {
            event.setDescription(updateDto.getDescription());
        }
        if (updateDto.getTitle() != null) {
            event.setTitle(updateDto.getTitle());
        }
        if (updateDto.getEventDate() != null) {
            event.setEventDate(updateDto.getEventDate());
        }
        if (updateDto.getParticipantLimit() != null) {
            event.setParticipantLimit(updateDto.getParticipantLimit());
        }
        if (updateDto.getPaid() != null) {
            event.setPaid(updateDto.getPaid());
        }
        if (updateDto.getRequestModeration() != null) {
            event.setRequestModeration(updateDto.getRequestModeration());
        }

        if (updateDto.getCategory() != null) {
            Category category = findCategoryOrThrow(updateDto.getCategory());
            event.setCategory(category);
        }

        if (updateDto.getLocation() != null) {
            Location location = new Location();
            location.setLat(updateDto.getLocation().getLat());
            location.setLon(updateDto.getLocation().getLon());
            event.setLocation(location);
        }

        if (updateDto.getStateActionUser() != null) {
            if (updateDto.getStateActionUser() == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (updateDto.getStateActionUser() == StateActionUser.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        Event savedEvent = eventRepository.save(event);
        return toEventFullDto(savedEvent);
    }

    public EventFullDto updateAdminEvent(Long eventId, AdminEventStateUpdateRequest request) {
        Event event = findEventOrThrow(eventId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тело запроса обязательно");
        }
        validateOptionalText("annotation", request.getAnnotation(), 20, 2000);
        validateOptionalText("description", request.getDescription(), 20, 7000);
        validateOptionalText("title", request.getTitle(), 3, 120);
        StateActionAdmin action = request.getStateActionAdmin();

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Дата события должна быть не раньше чем через час");
            }
            event.setEventDate(request.getEventDate());
        }
        if (request.getParticipantLimit() != null) {
            if (request.getParticipantLimit() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "participantLimit не может быть отрицательным");
            }
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getCategory() != null) {
            event.setCategory(findCategoryOrThrow(request.getCategory()));
        }
        if (request.getLocation() != null) {
            Location location = new Location();
            location.setLat(request.getLocation().getLat());
            location.setLon(request.getLocation().getLon());
            event.setLocation(location);
        }

        if (action != null) {
            if (action == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new EventConflictException("Опубликовать можно только событие PENDING");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (action == StateActionAdmin.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new EventConflictException("Нельзя отклонить уже опубликованное событие");
                }
                event.setState(EventState.CANCELED);
            }
        }

        Event savedEvent = eventRepository.save(event);
        return toEventFullDto(savedEvent);
    }

    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states,
                                             List<Long> categories, LocalDateTime rangeStart,
                                             LocalDateTime rangeEnd, int from, int size) {
        validateDateRange(rangeStart, rangeEnd);
        if (from < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from должен быть >= 0, size должен быть >= 1");
        }

        List<Event> events = eventRepository.findAll();

        if (users != null && !users.isEmpty()) {
            events = events.stream()
                    .filter(event -> users.contains(event.getInitiator().getId()))
                    .collect(Collectors.toList());
        }
        if (states != null && !states.isEmpty()) {
            events = events.stream()
                    .filter(event -> states.contains(event.getState()))
                    .collect(Collectors.toList());
        }
        if (categories != null && !categories.isEmpty()) {
            events = events.stream()
                    .filter(event -> categories.contains(event.getCategory().getId()))
                    .collect(Collectors.toList());
        }
        if (rangeStart != null) {
            events = events.stream()
                    .filter(event -> !event.getEventDate().isBefore(rangeStart))
                    .collect(Collectors.toList());
        }
        if (rangeEnd != null) {
            events = events.stream()
                    .filter(event -> !event.getEventDate().isAfter(rangeEnd))
                    .collect(Collectors.toList());
        }

        int start = Math.min(from, events.size());
        int end = (int) Math.min((long) from + size, events.size());
        List<Event> page = events.subList(start, end);
        Map<String, Long> viewsByUri = getViewsForEvents(page);

        List<EventFullDto> result = new ArrayList<>();
        for (Event event : page) {
            result.add(toEventFullDto(event,
                    viewsByUri.getOrDefault("/events/" + event.getId(), 0L)));
        }
        return result;
    }

    private void validateDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "rangeStart должен быть не позже rangeEnd");
        }
    }

    private EventFullDto toEventFullDto(Event event){
        String eventUri = "/events/" + event.getId();
        long views = getViewsByUri(List.of(eventUri)).getOrDefault(eventUri, 0L);
        return toEventFullDto(event, views);
    }

    private EventFullDto toEventFullDto(Event event, long views) {
        EventFullDto eventFullDto = new EventFullDto();
        eventFullDto.setId(event.getId());
        eventFullDto.setAnnotation(event.getAnnotation());
        eventFullDto.setDescription(event.getDescription());
        eventFullDto.setTitle(event.getTitle());
        eventFullDto.setEventDate(event.getEventDate());
        eventFullDto.setPaid(event.isPaid());
        eventFullDto.setParticipantLimit(event.getParticipantLimit());
        eventFullDto.setRequestModeration(event.isRequestModeration());
        eventFullDto.setCreatedOn(event.getCreatedOn());
        eventFullDto.setState(event.getState());

        eventFullDto.setConfirmedRequests(getConfirmedRequests(event.getId()));
        eventFullDto.setViews(views);

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(event.getCategory().getId());
        categoryDto.setName(event.getCategory().getName());
        eventFullDto.setCategory(categoryDto);

        UserShortDto initiatorDto = new UserShortDto();
        initiatorDto.setId(event.getInitiator().getId());
        initiatorDto.setName(event.getInitiator().getName());
        eventFullDto.setInitiator(initiatorDto);

        LocationDto locationDto = new LocationDto();
        locationDto.setLat(event.getLocation().getLat());
        locationDto.setLon(event.getLocation().getLon());
        eventFullDto.setLocation(locationDto);
        eventFullDto.setPublishedOn(event.getPublishedOn());

        return eventFullDto;
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        findUserOrThrow(userId);

        if (from < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from должен быть >= 0, size должен быть >= 1");
        }

        List<Event> events = eventRepository.findAllByInitiatorId(userId);
        int start = Math.min(from, events.size());
        int end = (int) Math.min((long) from + size, events.size());
        events = new ArrayList<>(events.subList(start, end));
        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto eventDto = new EventShortDto();

            eventDto.setId(event.getId());
            eventDto.setEventDate(event.getEventDate());
            eventDto.setTitle(event.getTitle());
            eventDto.setAnnotation(event.getAnnotation());
            eventDto.setPaid(event.isPaid());

            eventDto.setViews(0L);
            eventDto.setConfirmedRequests(getConfirmedRequests(event.getId()));

            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(event.getCategory().getId());
            categoryDto.setName(event.getCategory().getName());
            eventDto.setCategory(categoryDto);

            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(event.getInitiator().getId());
            initiatorDto.setName(event.getInitiator().getName());
            eventDto.setInitiator(initiatorDto);

            result.add(eventDto);
        }

        Map<String, Long> viewsByUri = getViewsForEvents(events);
        for (EventShortDto eventDto : result) {
            eventDto.setViews(viewsByUri.getOrDefault("/events/" + eventDto.getId(), 0L));
        }

        return result;
    }

    private void validateNewEvent(NewEventDto event) {
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тело запроса обязательно");
        }
        validateRequiredText("annotation", event.getAnnotation(), 20, 2000);
        validateRequiredText("description", event.getDescription(), 20, 7000);
        validateRequiredText("title", event.getTitle(), 3, 120);
        if (event.getCategory() == null || event.getEventDate() == null
                || event.getLocation() == null || event.getLocation().getLat() == null
                || event.getLocation().getLon() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "category, eventDate, location.lat и location.lon обязательны");
        }
        if (event.getParticipantLimit() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "participantLimit не может быть отрицательным");
        }
    }

    private void validateUpdateEvent(UpdateEventUserRequest event) {
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тело запроса обязательно");
        }
        validateOptionalText("annotation", event.getAnnotation(), 20, 2000);
        validateOptionalText("description", event.getDescription(), 20, 7000);
        validateOptionalText("title", event.getTitle(), 3, 120);
        if (event.getParticipantLimit() != null && event.getParticipantLimit() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "participantLimit не может быть отрицательным");
        }
        if (event.getLocation() != null && (event.getLocation().getLat() == null
                || event.getLocation().getLon() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Если location передан, lat и lon обязательны");
        }
    }

    private void validateRequiredText(String field, String value, int minLength, int maxLength) {
        if (value == null || value.isBlank() || value.length() < minLength || value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " должен содержать от " + minLength + " до " + maxLength + " символов");
        }
    }

    private void validateOptionalText(String field, String value, int minLength, int maxLength) {
        if (value != null && (value.isBlank() || value.length() < minLength || value.length() > maxLength)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " должен содержать от " + minLength + " до " + maxLength + " символов");
        }
    }
}
