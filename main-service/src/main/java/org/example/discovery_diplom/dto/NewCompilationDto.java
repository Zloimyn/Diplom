package org.example.discovery_diplom.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NewCompilationDto {
    private List<Long> events;
    private boolean pinned;
    private String title;
}
