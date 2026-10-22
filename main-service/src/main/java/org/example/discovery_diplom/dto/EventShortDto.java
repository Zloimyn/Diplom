package org.example.discovery_diplom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventShortDto {
    private Long id;
    private Long views;
    private Long confirmedRequests;

    private String annotation;
    private String title;

    private CategoryDto category;
    private UserShortDto initiator;
    private boolean paid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
}
