package com.example.demo.controller;

import com.trustify.trustify.entity.User;
import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.enums.UserRole;
import com.trustify.trustify.service.Business.UserBusinessService;
import com.trustify.trustify.service.Customer.UserService;
import com.trustify.trustify.service.RedisService;
import com.trustify.trustify.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final RedisService redisService;
    private final JwtUtil jwtUtil;
    private final UserBusinessService userBusinessService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/exchange-token")
    public ResponseEntity<TokenResponse> exchangeToken(
            @RequestBody StateRequest request,
            HttpServletResponse response) {
        String jwtToken = redisService.getAndRemoveStateToken(request.getState());
        if (jwtToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse("", "Invalid or expired state code"));
        }

        // Gửi token qua HttpOnly Cookie
        Cookie jwtCookie = new Cookie("access_token", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(3600);
        jwtCookie.setAttribute("SameSite", "None");
        response.addCookie(jwtCookie);
        return ResponseEntity.ok(new TokenResponse(jwtToken, "Token set successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("access_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        jwtCookie.setAttribute("SameSite", "None");
        response.addCookie(jwtCookie);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/magic-link")
    public ResponseEntity<String> sendmagicLink(@RequestParam String email) {
        redisService.sendMagicLink(email);
        return ResponseEntity.ok("Magic link sent to " + email);
    }

    @GetMapping("/magic-link/{code}")
    public ResponseEntity<TokenResponse> magicLink(@PathVariable String code, HttpServletResponse response) {
        String email = redisService.verifyMagicLink(code);
        if (email.equals("")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse("", "Invalid or expired state code"));
        }
        UserBusiness userBusiness = userBusinessService.findByEmail(email);
        String jwtToken = jwtUtil.generateBusinessToken(userBusiness);
        Cookie jwtCookie = new Cookie("access_token", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(3600);
        jwtCookie.setAttribute("SameSite", "None");
        response.addCookie(jwtCookie);
        return ResponseEntity.ok(new TokenResponse(jwtToken, "Token set successfully"));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletResponse response) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        String jwtToken = jwtUtil.generateToken(user, UserRole.ADMIN);

        // Set JWT in HttpOnly cookie
        Cookie jwtCookie = new Cookie("access_token", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(3600); // 1 hour
        jwtCookie.setAttribute("SameSite", "None");
        response.addCookie(jwtCookie);

        return ResponseEntity.ok(Map.of(
                "message", "Login successful"
        ));
    }




    @Data
    public static class StateRequest {
        private String state;
    }

    @Data
    @AllArgsConstructor
    public static class TokenResponse {
        private String token;
        private String error;
    }

}
