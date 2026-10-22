package org.example.discovery_diplom.controller;

import org.example.discovery_diplom.dto.CommentDto;
import org.example.discovery_diplom.dto.ModerateCommentRequest;
import org.example.discovery_diplom.model.CommentStatus;
import org.example.discovery_diplom.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentDto> getComments(@RequestParam(required = false) Long eventId,
                                        @RequestParam(required = false) CommentStatus status,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        return commentService.getAdminComments(eventId, status, from, size);
    }

    @PatchMapping("/{commentId}")
    public CommentDto moderateComment(@PathVariable Long commentId,
                                     @RequestBody ModerateCommentRequest request) {
        return commentService.moderateComment(commentId, request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
    }
}
