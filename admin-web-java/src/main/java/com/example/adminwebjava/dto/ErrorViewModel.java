package com.example.adminwebjava.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorViewModel {
    
    private String requestId;

    public boolean isShowRequestId() {
        return requestId != null && !requestId.trim().isEmpty();
    }
}