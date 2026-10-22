package org.example.discovery_diplom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NewEventDto {
    private int participantLimit;
    private Long category;           // id категории из запроса
    private LocationDto location;

    private boolean requestModeration = true;
    private boolean paid;

    private String annotation;
    private String description;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
}
