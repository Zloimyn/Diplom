package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.AdminEventStateUpdateRequest;
import org.example.discovery_diplom.dto.EventFullDto;
import org.example.discovery_diplom.dto.LocationDto;
import org.example.discovery_diplom.dto.NewEventDto;
import org.example.discovery_diplom.dto.StateActionAdmin;
import org.example.discovery_diplom.exception.EventConflictException;
import org.example.discovery_diplom.model.Category;
import org.example.discovery_diplom.model.Event;
import org.example.discovery_diplom.model.EventState;
import org.example.discovery_diplom.model.Location;
import org.example.discovery_diplom.model.User;
import org.example.discovery_diplom.exception.EventConflictException;
import org.example.discovery_diplom.repository.CategoryRepository;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.ParticipationRequestRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.example.discovery_diplom.stats.client.StatsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceAdminUpdateTest {

    private EventRepository eventRepository;
    private ParticipationRequestRepository participationRequestRepository;
    private EventService eventService;
    private Event event;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(EventRepository.class);
        CategoryRepository categoryRepository = Mockito.mock(CategoryRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        participationRequestRepository = Mockito.mock(ParticipationRequestRepository.class);
        StatsClient statsClient = Mockito.mock(StatsClient.class);

        eventService = new EventService(eventRepository, categoryRepository, userRepository,
                participationRequestRepository, statsClient);

        User initiator = new User();
        initiator.setId(5L);
        initiator.setName("Организатор");

        Category category = new Category();
        category.setId(1L);
        category.setName("Концерты");

        event = new Event();
        event.setId(10L);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setState(EventState.PENDING);
        event.setTitle("Старое название");
        event.setAnnotation("Аннотация");
        event.setDescription("Описание");
        Location location = new Location();
        location.setLat(55.75);
        location.setLon(37.61);
        event.setLocation(location);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRequestRepository.countByEvent_IdAndStatus(any(), any())).thenReturn(0L);
        when(statsClient.getStats(any())).thenReturn(List.of());
    }

    @Test
    void updateAdminEventChangesOnlyFieldsProvided() {
        AdminEventStateUpdateRequest request = new AdminEventStateUpdateRequest();
        request.setTitle("Новое название");

        EventFullDto result = eventService.updateAdminEvent(10L, request);

        assertEquals("Новое название", result.getTitle());
        assertEquals("Аннотация", result.getAnnotation());
        assertEquals("Описание", result.getDescription());
        verify(eventRepository).save(event);
    }

    @Test
    void publishPendingEventSetsPublishedStateAndTime() {
        AdminEventStateUpdateRequest request = new AdminEventStateUpdateRequest();
        request.setStateActionAdmin(StateActionAdmin.PUBLISH_EVENT);

        EventFullDto result = eventService.updateAdminEvent(10L, request);

        assertEquals(EventState.PUBLISHED, result.getState());
        assertNotNull(result.getPublishedOn());
    }

    @Test
    void cannotPublishEventThatIsNotPending() {
        event.setState(EventState.PUBLISHED);
        AdminEventStateUpdateRequest request = new AdminEventStateUpdateRequest();
        request.setStateActionAdmin(StateActionAdmin.PUBLISH_EVENT);

        assertThrows(EventConflictException.class,
                () -> eventService.updateAdminEvent(10L, request));
    }

    @Test
    void adminCannotMoveEventToLessThanOneHourFromNow() {
        AdminEventStateUpdateRequest request = new AdminEventStateUpdateRequest();
        request.setEventDate(LocalDateTime.now().plusMinutes(30));

        assertThrows(EventConflictException.class,
                () -> eventService.updateAdminEvent(10L, request));
    }

    @Test
    void userCannotCreateEventLessThanTwoHoursFromNow() {
        NewEventDto request = new NewEventDto();
        request.setTitle("Подходящее название");
        request.setAnnotation("Достаточно длинная аннотация события");
        request.setDescription("Достаточно длинное описание события для проверки");
        request.setCategory(1L);
        request.setEventDate(LocalDateTime.now().plusHours(1));

        LocationDto newLocation = new LocationDto();
        newLocation.setLat(55.75);
        newLocation.setLon(37.61);
        request.setLocation(newLocation);

        assertThrows(EventConflictException.class,
                () -> eventService.createEvent(5L, request));
    }
}
