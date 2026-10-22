package org.example.discovery_diplom.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.discovery_diplom.model.RequestStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class ParticipationRequestDto {
    private LocalDateTime created;
    private Long event;
    private Long id;
    private Long requester;
    private RequestStatus status;
}
