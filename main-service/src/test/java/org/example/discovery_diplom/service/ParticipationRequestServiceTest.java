package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.EventRequestStatusUpdateRequest;
import org.example.discovery_diplom.dto.EventRequestStatusUpdateResult;
import org.example.discovery_diplom.model.Event;
import org.example.discovery_diplom.model.EventState;
import org.example.discovery_diplom.model.ParticipationRequest;
import org.example.discovery_diplom.model.RequestStatus;
import org.example.discovery_diplom.model.User;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.ParticipationRequestRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipationRequestServiceTest {

    private ParticipationRequestRepository requestRepository;
    private ParticipationRequestService service;
    private Event event;
    private ParticipationRequest requestToConfirm;
    private ParticipationRequest requestToRejectAutomatically;

    @BeforeEach
    void setUp() {
        requestRepository = Mockito.mock(ParticipationRequestRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        EventRepository eventRepository = Mockito.mock(EventRepository.class);
        service = new ParticipationRequestService(requestRepository, userRepository, eventRepository);

        User organizer = new User();
        organizer.setId(5L);
        organizer.setName("Организатор");

        User requester = new User();
        requester.setId(6L);
        requester.setName("Участник");

        event = new Event();
        event.setId(10L);
        event.setInitiator(organizer);
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(2);

        requestToConfirm = createRequest(21L, requester, event, RequestStatus.PENDING);
        requestToRejectAutomatically = createRequest(22L, requester, event, RequestStatus.PENDING);

        when(userRepository.findById(5L)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(anyList())).thenReturn(List.of(requestToConfirm));
        when(requestRepository.countByEvent_IdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(1L);
        when(requestRepository.findAllByEventIdAndStatus(10L, RequestStatus.PENDING))
                .thenReturn(List.of(requestToRejectAutomatically));
        when(requestRepository.save(any(ParticipationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reachingParticipantLimitRejectsOtherPendingRequests() {
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest();
        update.setRequestIds(List.of(21L));
        update.setStatus(RequestStatus.CONFIRMED);

        EventRequestStatusUpdateResult result = service.updateRequestsStatus(5L, 10L, update);

        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals(21L, result.getConfirmedRequests().get(0).getId());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals(22L, result.getRejectedRequests().get(0).getId());
        assertEquals(RequestStatus.REJECTED, requestToRejectAutomatically.getStatus());
        verify(requestRepository).findAllByEventIdAndStatus(10L, RequestStatus.PENDING);
    }

    @Test
    void cannotConfirmWhenParticipantLimitIsAlreadyFull() {
        when(requestRepository.countByEvent_IdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(2L);

        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest();
        update.setRequestIds(List.of(21L));
        update.setStatus(RequestStatus.CONFIRMED);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateRequestsStatus(5L, 10L, update));

        assertEquals(409, exception.getStatusCode().value());
        Mockito.verify(requestRepository, Mockito.never()).save(any(ParticipationRequest.class));
    }

    private ParticipationRequest createRequest(Long id, User requester, Event event, RequestStatus status) {
        ParticipationRequest request = new ParticipationRequest();
        request.setId(id);
        request.setRequester(requester);
        request.setEvent(event);
        request.setStatus(status);
        return request;
    }
}
