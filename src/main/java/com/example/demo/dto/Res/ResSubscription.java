package com.example.demo.dto.Res;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResSubscription {
    private Long subscriptionId;
    private String planId;
    private String planName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
