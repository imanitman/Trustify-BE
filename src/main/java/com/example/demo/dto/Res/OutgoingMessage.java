package com.example.demo.dto.Res;
import lombok.Data;
import java.time.Instant;

@Data
public class OutgoingMessage {
    private Long id;
    private Long roomId;
    private String sender;
    private String message;
    private boolean isAdmin;
    private Instant timestamp;
}
