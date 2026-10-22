package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.EventRequestStatusUpdateRequest;
import org.example.discovery_diplom.dto.EventRequestStatusUpdateResult;
import org.example.discovery_diplom.dto.ParticipationRequestDto;
import org.example.discovery_diplom.model.*;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.ParticipationRequestRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public ParticipationRequestService(ParticipationRequestRepository requestRepository, UserRepository userRepository, EventRepository eventRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Событие не опубликовано");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Нельзя подать заявку на своё событие");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Заявка уже существует");
        }

        long confirmedCount = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Достигнут лимит участников");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequester(user);
        request.setEvent(event);
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));

        RequestStatus status;

        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        request.setStatus(status);

        ParticipationRequest savedRequest = requestRepository.save(request);

        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(savedRequest.getId());
        dto.setCreated(savedRequest.getCreated());
        dto.setEvent(savedRequest.getEvent().getId());
        dto.setRequester(savedRequest.getRequester().getId());
        dto.setStatus(savedRequest.getStatus());

        return dto;
    }

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);

        List<ParticipationRequestDto> result = new java.util.ArrayList<>();

        for (ParticipationRequest request : requests) {
            ParticipationRequestDto dto = new ParticipationRequestDto();
            dto.setId(request.getId());
            dto.setCreated(request.getCreated());
            dto.setEvent(request.getEvent().getId());
            dto.setRequester(request.getRequester().getId());
            dto.setStatus(request.getStatus());
            result.add(dto);
        }

        return result;
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        User organizer = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));

        if (!event.getInitiator().getId().equals(organizer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено");
        }

        List<ParticipationRequestDto> result = new java.util.ArrayList<>();

        for (ParticipationRequest request : requestRepository.findAllByEventId(eventId)) {
            ParticipationRequestDto dto = new ParticipationRequestDto();
            dto.setId(request.getId());
            dto.setCreated(request.getCreated());
            dto.setEvent(request.getEvent().getId());
            dto.setRequester(request.getRequester().getId());
            dto.setStatus(request.getStatus());
            result.add(dto);
        }

        return result;
    }

    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest update) {
        if (update == null || update.getRequestIds() == null || update.getRequestIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "requestIds должен содержать хотя бы один идентификатор");
        }

        User organizer = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Событие не найдено"));

        if (!event.getInitiator().getId().equals(organizer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено");
        }

        if (update.getStatus() != RequestStatus.CONFIRMED
                && update.getStatus() != RequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недопустимый статус");
        }

        List<ParticipationRequest> requests =
                requestRepository.findAllById(update.getRequestIds());

        if (requests.size() != update.getRequestIds().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка относится к другому событию");
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Изменять можно только заявки со статусом PENDING");
            }
        }

        long confirmedCount = requestRepository.countByEvent_IdAndStatus(
                eventId, RequestStatus.CONFIRMED);

        if (update.getStatus() == RequestStatus.CONFIRMED
                && event.getParticipantLimit() > 0
                && confirmedCount + requests.size() > event.getParticipantLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Превышен лимит участников");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(new ArrayList<>());
        result.setRejectedRequests(new ArrayList<>());

        for (ParticipationRequest request : requests) {
            request.setStatus(update.getStatus());
            ParticipationRequest saved = requestRepository.save(request);

            ParticipationRequestDto dto = new ParticipationRequestDto();
            dto.setId(saved.getId());
            dto.setCreated(saved.getCreated());
            dto.setEvent(saved.getEvent().getId());
            dto.setRequester(saved.getRequester().getId());
            dto.setStatus(saved.getStatus());

            if (saved.getStatus() == RequestStatus.CONFIRMED) {
                result.getConfirmedRequests().add(dto);
            } else {
                result.getRejectedRequests().add(dto);
            }
        }

        if (update.getStatus() == RequestStatus.CONFIRMED
                && event.getParticipantLimit() > 0
                && confirmedCount + requests.size() == event.getParticipantLimit()) {
            List<ParticipationRequest> remainingRequests = requestRepository
                    .findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);

            for (ParticipationRequest request : remainingRequests) {
                request.setStatus(RequestStatus.REJECTED);
                ParticipationRequest saved = requestRepository.save(request);

                ParticipationRequestDto dto = new ParticipationRequestDto();
                dto.setId(saved.getId());
                dto.setCreated(saved.getCreated());
                dto.setEvent(saved.getEvent().getId());
                dto.setRequester(saved.getRequester().getId());
                dto.setStatus(saved.getStatus());
                result.getRejectedRequests().add(dto);
            }
        }

        return result;
    }

    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        ParticipationRequest request = requestRepository.findById(requestId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));

        if (!request.getRequester().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest savedRequest = requestRepository.save(request);

        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(savedRequest.getId());
        dto.setCreated(savedRequest.getCreated());
        dto.setEvent(savedRequest.getEvent().getId());
        dto.setRequester(savedRequest.getRequester().getId());
        dto.setStatus(savedRequest.getStatus());

        return dto;
    }
}
