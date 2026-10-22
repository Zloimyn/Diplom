package org.example.discovery_diplom.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.discovery_diplom.model.RequestStatus;

import java.util.List;

@Getter
@Setter
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private RequestStatus status;
}
