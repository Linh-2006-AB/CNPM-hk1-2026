package com.example.adminwebjava.dto.view;

import com.example.adminwebjava.entity.VisitorPlaybackLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageHistoryViewModel {
    private List logs = new ArrayList<>();
}