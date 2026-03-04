package com.example.demo.dto.Res;

import lombok.Data;
import java.time.Instant;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String sender;
    private Instant timestamp;
    private boolean read;
}
