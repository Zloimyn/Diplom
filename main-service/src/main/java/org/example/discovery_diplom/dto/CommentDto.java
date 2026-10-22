package org.example.discovery_diplom.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.discovery_diplom.model.CommentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentDto {
    private Long id;
    private Long eventId;
    private UserShortDto author;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CommentStatus status;
}
