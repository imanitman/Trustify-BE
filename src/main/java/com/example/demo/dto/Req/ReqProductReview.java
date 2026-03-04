package com.example.demo.dto.Req;

import lombok.Data;

@Data
public class ReqProductReview {
    private String productCode;
    private String review;
    private Long companyId;
    private String email;
}
