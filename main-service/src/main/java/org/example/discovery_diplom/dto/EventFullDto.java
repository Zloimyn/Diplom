package org.example.discovery_diplom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.example.discovery_diplom.model.EventState;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventFullDto {
    private int participantLimit;
    private Long id;
    private Long confirmedRequests;
    private Long views;


    private String annotation;
    private String description;
    private String title;

    private boolean paid;
    private boolean requestModeration;

    private CategoryDto category;
    private EventState state;
    private LocationDto location;
    private UserShortDto initiator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;
}
