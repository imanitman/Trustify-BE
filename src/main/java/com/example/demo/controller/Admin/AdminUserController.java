package com.example.demo.controller.Admin;

import com.trustify.trustify.dto.Req.AdminDto;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.UserStatus;
import com.trustify.trustify.service.Customer.UserService;
import com.trustify.trustify.service.RedisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/user")
@AllArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService;
    private final RedisService redisService;


    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam (defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> response = new HashMap<>();
        try{
            Page<User> userPage = userService.getAllUsers(page, size); // List<User>
            response.put("success", true);
            response.put("users", userPage.getContent()); // List<User>
            response.put("currentPage", userPage.getNumber());
            response.put("totalPages", userPage.getTotalPages());
            response.put("totalItems", userPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting users", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/status")
    public ResponseEntity<User> updateUserStatus(@RequestParam String email, @RequestParam UserStatus status) {
        try {
            User user = userService.updateUserStatusByEmail(email, status);
            if (status == UserStatus.INACTIVE) {
                redisService.saveInactiveUser(email);
            }
            else {
                redisService.removeInactiveUser(email);
            }
            return ResponseEntity.ok(user);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody AdminDto adminDto) {
        User user = userService.createUserAdmin(adminDto);
        return ResponseEntity.ok(user);
    }
}
