package com.example.demo.controller.Customer;

import com.trustify.trustify.dto.Req.ReqUserDto;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.UserStatus;
import com.trustify.trustify.service.Customer.UserService;
import com.trustify.trustify.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final RedisService redisService;

    @GetMapping("/me")
    public ResponseEntity<User> getUserByEmail(HttpServletRequest request) {
        try {
            User user = userService.getCurrentUser(request);
            return ResponseEntity.ok(user);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateUser(
            HttpServletRequest request,
            @RequestBody ReqUserDto userDto) {
        try {
            User currentUser = userService.getCurrentUser(request);
            User updatedUser = userService.updateUser(userDto);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}

