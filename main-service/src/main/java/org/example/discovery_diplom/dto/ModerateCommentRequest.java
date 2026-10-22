package org.example.discovery_diplom.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.discovery_diplom.model.CommentStatus;

@Getter
@Setter
public class ModerateCommentRequest {
    private CommentStatus status;
}
