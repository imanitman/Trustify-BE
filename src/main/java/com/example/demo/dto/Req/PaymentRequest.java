package com.example.demo.dto.Req;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long companyId;
    private Long planId;
    private String bankCode;  // Optional: NCB, VNPAYQR, etc.
}
