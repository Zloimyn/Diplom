package org.example.discovery_diplom.controller;

import org.example.discovery_diplom.dto.CommentDto;
import org.example.discovery_diplom.dto.NewCommentRequest;
import org.example.discovery_diplom.dto.UpdateCommentRequest;
import org.example.discovery_diplom.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}")
public class UserCommentController {
    private final CommentService commentService;

    public UserCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @RequestBody NewCommentRequest request) {
        return commentService.addComment(userId, eventId, request);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(userId, commentId, request);
    }
}
