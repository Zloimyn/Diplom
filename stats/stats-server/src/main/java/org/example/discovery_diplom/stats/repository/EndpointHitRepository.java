package org.example.discovery_diplom.stats.repository;

import org.example.discovery_diplom.stats.model.EndpointHit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.discovery_diplom.stats.dto.ViewStatsDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {
    // Просто подсчет айпи не уникальный
    @Query("""
        select new org.example.discovery_diplom.stats.dto.ViewStatsDto(hit.app, hit.uri, count(hit))
        from EndpointHit hit where hit.timestamp >= :start and hit.timestamp <= :end
        group by hit.app, hit.uri
        order by count(hit) desc
        """)
    List<ViewStatsDto> findStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Уникальные айпи
    @Query("""
        select new org.example.discovery_diplom.stats.dto.ViewStatsDto(hit.app, hit.uri, count(distinct hit.ip))
        from EndpointHit hit where hit.timestamp >= :start and hit.timestamp <= :end
        group by hit.app, hit.uri
        order by count(distinct hit.ip) desc
        """)
    List<ViewStatsDto> findUniqueStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
