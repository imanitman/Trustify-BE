package com.example.demo.util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user, UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", role.name());
        claims.put("name", user.getName());
        claims.put("provider", user.getProvider().name());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateBusinessToken(UserBusiness userBusiness) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userBusiness.getId());
        claims.put("email", userBusiness.getEmail());
        claims.put("role", userBusiness.getRole().name());
        claims.put("company", userBusiness.getCompany().getName());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userBusiness.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // file: 'src/main/java/com/trustify/trustify/util/JwtUtil.java'
    public Long extractUserId(String token) {
        Object value = extractClaims(token).get("userId");
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }


    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token, String email) {
        return (extractEmail(token).equals(email) && !isTokenExpired(token));
    }
    public String getEmailFromToken(String token) {
        return extractEmail(token);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        Object rolesObj = claims.get("role");
        if (rolesObj == null) {
            rolesObj = claims.get("roles");
        }
        if (rolesObj == null) {
            return List.of();
        }

        // Handle String first (most common case for your tokens)
        if (rolesObj instanceof String) {
            String s = ((String) rolesObj).trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                try {
                    return objectMapper.readValue(s, new TypeReference<List<String>>() {});
                } catch (Exception ignored) { }
            }
            if (s.contains(",")) {
                return Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toList());
            }
            return List.of(s); // Single role as list
        }

        // Handle List (for future compatibility)
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return List.of(rolesObj.toString());
    }

}
