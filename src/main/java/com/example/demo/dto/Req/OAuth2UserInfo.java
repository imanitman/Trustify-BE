package com.example.demo.dto.Req;

import com.trustify.trustify.enums.AuthProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuth2UserInfo {
    private String email;
    private String name;
    private String avatarUrl;
    private String providerId;
    private AuthProvider provider;
}

