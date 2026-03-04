package com.example.demo.dto.Res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String status;
    private String message;
    private String paymentUrl;
    private String txnRef;
}