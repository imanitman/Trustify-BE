package com.example.demo.dto.Res;

import lombok.Data;

@Data
public class ResUserDto {
    private Long id;
    private String email;
    private String role;
    private String name;
    private String profileImg;
}
