package org.example.discovery_diplom.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminEventStateUpdateRequest {
    private Integer participantLimit;
    private Long category;
    private LocationDto location;

    private Boolean requestModeration;
    private Boolean paid;

    private String annotation;
    private String description;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @JsonAlias("stateAction")
    private StateActionAdmin stateActionAdmin;
}
