package org.example.discovery_diplom.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Location {
    private Double lat;
    private Double lon;
}
