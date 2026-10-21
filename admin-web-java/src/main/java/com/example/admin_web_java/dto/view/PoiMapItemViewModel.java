package com.example.adminwebjava.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiMapItemViewModel {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name = "";

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("radius")
    private Integer radius;

    @JsonProperty("status")
    private String status = "";
}