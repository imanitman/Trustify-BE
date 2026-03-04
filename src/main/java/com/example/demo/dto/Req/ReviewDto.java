package com.example.demo.dto.Req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trustify.trustify.enums.ReviewStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewDto {
    private Long id;
    private String title;
    private String description;
    private String reply;
    private String email;
    private boolean love;
    private boolean likes;
    private String status;
    private String contendReport;
    private String companyName;
    private Integer rating;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expDate;
}
