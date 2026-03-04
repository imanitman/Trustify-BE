package com.example.demo.dto.Res;

import lombok.Data;

@Data
public class RoomChatDTO {
    private Long id;
    private String name;
    private Long userBusinessId;
    private String userBusinessName;
    private Integer messageCount;
}
