package org.example.discovery_diplom.stats.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViewStatsDto {
    private String app;
    private String uri;
    private long hits; // Число просмотров

    public ViewStatsDto() {}

    public ViewStatsDto(String app, String uri, long hits) {
        this.app = app;
        this.uri = uri;
        this.hits = hits;
    }
}
