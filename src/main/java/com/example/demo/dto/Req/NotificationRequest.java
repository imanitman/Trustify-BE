package com.example.demo.dto.Req;

import lombok.Data;

@Data
public class NotificationRequest {
    private String title;
    private String message;
    private String type;        // INFO, WARNING, ERROR, SUCCESS
    private String targetUser;  // null = broadcast to all
}
