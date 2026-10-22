package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.CommentDto;
import org.example.discovery_diplom.dto.ModerateCommentRequest;
import org.example.discovery_diplom.dto.NewCommentRequest;
import org.example.discovery_diplom.dto.UpdateCommentRequest;
import org.example.discovery_diplom.dto.UserShortDto;
import org.example.discovery_diplom.exception.EventNotFoundException;
import org.example.discovery_diplom.exception.UserNotFoundException;
import org.example.discovery_diplom.model.Comment;
import org.example.discovery_diplom.model.CommentStatus;
import org.example.discovery_diplom.model.Event;
import org.example.discovery_diplom.model.EventState;
import org.example.discovery_diplom.model.User;
import org.example.discovery_diplom.repository.CommentRepository;
import org.example.discovery_diplom.repository.EventRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private static final int MAX_TEXT_LENGTH = 2000;

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          EventRepository eventRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public CommentDto addComment(Long userId, Long eventId, NewCommentRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Комментировать можно только опубликованные события");
        }
        validateText(request == null ? null : request.getText());

        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setEvent(event);
        comment.setAuthor(author);
        comment.setText(request.getText().trim());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setStatus(CommentStatus.PENDING);
        return toDto(commentRepository.save(comment));
    }

    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentRequest request) {
        Comment comment = findComment(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден");
        }
        validateText(request == null ? null : request.getText());

        comment.setText(request.getText().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        return toDto(commentRepository.save(comment));
    }

    public List<CommentDto> getPublicComments(Long eventId, int from, int size) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Событие с id=" + eventId + " не найдено"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new EventNotFoundException("Событие с id=" + eventId + " не найдено");
        }
        Pageable pageable = createPageable(from, size);
        return commentRepository.findAllByEvent_IdAndStatus(eventId, CommentStatus.PUBLISHED, pageable)
                .map(this::toDto).getContent();
    }

    public List<CommentDto> getAdminComments(Long eventId, CommentStatus status, int from, int size) {
        Pageable pageable = createPageable(from, size);
        Page<Comment> comments;
        if (eventId != null && status != null) {
            comments = commentRepository.findAllByEvent_IdAndStatus(eventId, status, pageable);
        } else if (eventId != null) {
            comments = commentRepository.findAllByEvent_Id(eventId, pageable);
        } else if (status != null) {
            comments = commentRepository.findAllByStatus(status, pageable);
        } else {
            comments = commentRepository.findAll(pageable);
        }
        return comments.map(this::toDto).getContent();
    }

    public CommentDto moderateComment(Long commentId, ModerateCommentRequest request) {
        Comment comment = findComment(commentId);
        if (request == null || (request.getStatus() != CommentStatus.PUBLISHED
                && request.getStatus() != CommentStatus.REJECTED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Статус модерации должен быть PUBLISHED или REJECTED");
        }
        comment.setStatus(request.getStatus());
        comment.setUpdatedAt(LocalDateTime.now());
        return toDto(commentRepository.save(comment));
    }

    public void deleteComment(Long commentId) {
        commentRepository.delete(findComment(commentId));
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Комментарий с id=" + commentId + " не найден"));
    }

    private Pageable createPageable(int from, int size) {
        if (from < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from должен быть >= 0, size должен быть >= 1");
        }
        return new OffsetPageRequest(from, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
    }

    private void validateText(String text) {
        if (text == null || text.isBlank() || text.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Текст комментария должен содержать от 1 до " + MAX_TEXT_LENGTH + " символов");
        }
    }

    private CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setEventId(comment.getEvent().getId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setStatus(comment.getStatus());

        UserShortDto author = new UserShortDto();
        author.setId(comment.getAuthor().getId());
        author.setName(comment.getAuthor().getName());
        dto.setAuthor(author);
        return dto;
    }
}
