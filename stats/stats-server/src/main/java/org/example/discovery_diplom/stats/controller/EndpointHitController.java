package org.example.discovery_diplom.stats.controller;

import org.example.discovery_diplom.stats.dto.EndpointHitDto;
import org.example.discovery_diplom.stats.service.EndpointHitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.example.discovery_diplom.stats.dto.ViewStatsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class EndpointHitController {
    private final EndpointHitService endpointHitService;

    public EndpointHitController(EndpointHitService endpointHitService) {
        this.endpointHitService = endpointHitService;
    }

    @PostMapping("/hit")
    public void saveHit(@RequestBody EndpointHitDto dto) {
        endpointHitService.saveHit(dto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(defaultValue = "false") boolean unique,
                                       @RequestParam(name = "uris", required = false) List<String> uris) {
        return endpointHitService.getStats(start, end, unique, uris);
    }
}
