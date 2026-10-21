package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorTrafficPoiOptionViewModel {

    private Integer poiId;
    private String poiName = "";
    private String ownerName = "";
    private Boolean hasMenu;
}