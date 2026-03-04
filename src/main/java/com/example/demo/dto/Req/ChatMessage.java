package com.example.demo.dto.Req;

import lombok.Data;

@Data
public class ChatMessage {
    private Long roomId;
    private boolean isAdmin;
    private String message;
}
