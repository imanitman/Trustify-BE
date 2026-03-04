package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trustify.trustify.dto.Req.OAuth2UserInfo;
import com.trustify.trustify.enums.AuthProvider;
import com.trustify.trustify.exception.OAuth2AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final RestTemplate restTemplate;

    @Value("${oauth2.google.client-id}")
    private String googleClientId;

    @Value("${oauth2.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth2.google.token-uri}")
    private String googleTokenUri;

    @Value("${oauth2.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${oauth2.facebook.client-id}")
    private String facebookClientId;

    @Value("${oauth2.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${oauth2.facebook.token-uri}")
    private String facebookTokenUri;

    @Value("${oauth2.facebook.user-info-uri}")
    private String facebookUserInfoUri;

    public OAuth2UserInfo getUserInfo(String provider, String code, String redirectUri) {
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());

        return switch (authProvider) {
            case GOOGLE -> getGoogleUserInfo(code, redirectUri);
            case FACEBOOK -> getFacebookUserInfo(code, redirectUri);
            case LOCAL -> throw new OAuth2AuthenticationException("LOCAL provider is not supported for OAuth2 flow");
        };
    }

    private OAuth2UserInfo getGoogleUserInfo(String code, String redirectUri) throws OAuth2AuthenticationException {
        try {
            String accessToken = getGoogleAccessToken(code, redirectUri);
            return fetchGoogleUserInfo(accessToken);
        } catch (Exception e) {
            log.error("Failed to get Google user info", e);
            throw new OAuth2AuthenticationException("Failed to authenticate with Google", e);
        }
    }

    private String getGoogleAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                googleTokenUri,
                request,
                JsonNode.class
        );
        if (response.getBody() == null || !response.getBody().has("access_token")) {
            throw new OAuth2AuthenticationException("Failed to get access token from Google");
        }
        return response.getBody().get("access_token").asText();
    }
    public OAuth2UserInfo extractUserInfo(OAuth2User oauth2User, String registrationId) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        if ("google".equals(registrationId)) {
            return OAuth2UserInfo.builder()
                    .email((String) attributes.get("email"))
                    .name((String) attributes.get("name"))
                    .provider(AuthProvider.valueOf("GOOGLE"))
                    .providerId((String) attributes.get("sub"))
                    .build();
        } else if ("facebook".equals(registrationId)) {
            return OAuth2UserInfo.builder()
                    .email((String) attributes.get("email"))
                    .name((String) attributes.get("name"))
                    .provider(AuthProvider.valueOf("FACEBOOK"))
                    .providerId((String) attributes.get("id"))
                    .build();
        }

        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }
    private OAuth2UserInfo fetchGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                googleUserInfoUri,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        JsonNode userInfo = response.getBody();
        if (userInfo == null) {
            throw new OAuth2AuthenticationException("Failed to fetch user info from Google");
        }

        return OAuth2UserInfo.builder()
                .email(userInfo.get("email").asText())
                .name(userInfo.get("name").asText())
                .avatarUrl(userInfo.has("picture") ? userInfo.get("picture").asText() : null)
                .providerId(userInfo.get("sub").asText())
                .provider(AuthProvider.GOOGLE)
                .build();
    }
    private OAuth2UserInfo getFacebookUserInfo(String code, String redirectUri) {
        try {
            String accessToken = getFacebookAccessToken(code, redirectUri);
            return fetchFacebookUserInfo(accessToken);
        } catch (Exception e) {
            log.error("Failed to get Facebook user info", e);
            throw new OAuth2AuthenticationException("Failed to authenticate with Facebook", e);
        }
    }
    private String getFacebookAccessToken(String code, String redirectUri) {
        String url = String.format(
                "%s?client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                facebookTokenUri,
                facebookClientId,
                facebookClientSecret,
                code,
                redirectUri
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        if (response.getBody() == null || !response.getBody().has("access_token")) {
            throw new OAuth2AuthenticationException("Failed to get access token from Facebook");
        }

        return response.getBody().get("access_token").asText();
    }

    private OAuth2UserInfo fetchFacebookUserInfo(String accessToken) {
        String url = String.format(
                "%s?fields=id,name,email,picture&access_token=%s",
                facebookUserInfoUri,
                accessToken
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        JsonNode userInfo = response.getBody();
        if (userInfo == null) {
            throw new OAuth2AuthenticationException("Failed to fetch user info from Facebook");
        }

        String avatarUrl = null;
        if (userInfo.has("picture") && userInfo.get("picture").has("data")) {
            JsonNode pictureData = userInfo.get("picture").get("data");
            if (pictureData.has("url")) {
                avatarUrl = pictureData.get("url").asText();
            }
        }

        return OAuth2UserInfo.builder()
                .email(userInfo.has("email") ? userInfo.get("email").asText() : null)
                .name(userInfo.get("name").asText())
                .avatarUrl(avatarUrl)
                .providerId(userInfo.get("id").asText())
                .provider(AuthProvider.FACEBOOK)
                .build();
    }
}