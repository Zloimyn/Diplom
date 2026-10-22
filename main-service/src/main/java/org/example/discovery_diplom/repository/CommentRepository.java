package org.example.discovery_diplom.repository;

import org.example.discovery_diplom.model.Comment;
import org.example.discovery_diplom.model.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByEvent_IdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    Page<Comment> findAllByStatus(CommentStatus status, Pageable pageable);

    Page<Comment> findAllByEvent_Id(Long eventId, Pageable pageable);

    boolean existsByAuthor_Id(Long authorId);

    void deleteAllByAuthor_Id(Long authorId);
}
