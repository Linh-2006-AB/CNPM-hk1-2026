package com.example.adminwebjava.dto.view;

import com.example.adminwebjava.entity.Tourist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorListItemViewModel {

    private Tourist tourist = new Tourist();
    private Integer totalPoints;
    private Integer discoveredPoiCount;
    private String rankName = "";
}