package org.example.discovery_diplom.repository;

import org.example.discovery_diplom.model.Event;
import org.example.discovery_diplom.model.EventState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event,Long> {
    List<Event> findAllByInitiatorId(Long userId);
    List<Event> findAllByState(EventState state);
    boolean existsByCategory_Id(Long categoryId);
}
