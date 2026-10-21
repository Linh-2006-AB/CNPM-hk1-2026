package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatReplyDto {
    private String reply = "";
    private String languageCode = "vi";
    private String source = "fallback";
}