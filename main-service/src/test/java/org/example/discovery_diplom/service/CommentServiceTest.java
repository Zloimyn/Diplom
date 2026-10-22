package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.CommentDto;
import org.example.discovery_diplom.dto.ModerateCommentRequest;
import org.example.discovery_diplom.dto.NewCommentRequest;
import org.example.discovery_diplom.dto.UpdateCommentRequest;
import org.example.discovery_diplom.model.Comment;
import org.example.discovery_diplom.model.CommentStatus;
import org.example.discovery_diplom.model.Event;
import org.example.discovery_diplom.model.EventState;
import org.example.discovery_diplom.model.User;
import org.example.discovery_diplom.repository.CommentRepository;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {
    private CommentRepository commentRepository;
    private CommentService commentService;
    private Event event;
    private User author;
    private Comment comment;

    @BeforeEach
    void setUp() {
        commentRepository = Mockito.mock(CommentRepository.class);
        EventRepository eventRepository = Mockito.mock(EventRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        commentService = new CommentService(commentRepository, eventRepository, userRepository);

        author = new User();
        author.setId(7L);
        author.setName("Комментатор");

        event = new Event();
        event.setId(12L);
        event.setState(EventState.PUBLISHED);

        comment = new Comment();
        comment.setId(31L);
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setText("Исходный текст");
        comment.setStatus(CommentStatus.PUBLISHED);

        when(userRepository.findById(7L)).thenReturn(Optional.of(author));
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(commentRepository.findById(31L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newCommentStartsPendingForModeration() {
        NewCommentRequest request = new NewCommentRequest();
        request.setText("Новый комментарий к событию");

        CommentDto result = commentService.addComment(7L, 12L, request);

        assertEquals(CommentStatus.PENDING, result.getStatus());
        assertEquals("Новый комментарий к событию", result.getText());
    }

    @Test
    void editedCommentReturnsToModeration() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setText("Исправленный текст комментария");

        CommentDto result = commentService.updateComment(7L, 31L, request);

        assertEquals(CommentStatus.PENDING, result.getStatus());
        assertEquals("Исправленный текст комментария", result.getText());
    }

    @Test
    void onlyPublishedCommentsAreRequestedForPublicList() {
        when(commentRepository.findAllByEvent_IdAndStatus(anyLong(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        List<CommentDto> result = commentService.getPublicComments(12L, 0, 10);

        assertEquals(1, result.size());
        verify(commentRepository).findAllByEvent_IdAndStatus(
                Mockito.eq(12L), Mockito.eq(CommentStatus.PUBLISHED), any());
    }

    @Test
    void adminCannotSetCommentBackToPending() {
        ModerateCommentRequest request = new ModerateCommentRequest();
        request.setStatus(CommentStatus.PENDING);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> commentService.moderateComment(31L, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
