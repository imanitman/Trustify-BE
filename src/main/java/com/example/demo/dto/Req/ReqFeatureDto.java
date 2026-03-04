package com.example.demo.dto.Req;

import lombok.Data;

import java.util.List;

@Data
public class ReqFeatureDto {
    private String name;
    private String description;
    private List<Long> plans; // List of plan IDs>
}
