package com.example.demo.service.Customer;


import com.trustify.trustify.dto.Req.AdminDto;
import com.trustify.trustify.dto.Req.OAuth2UserInfo;
import com.trustify.trustify.dto.Req.ReqUserDto;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.AuthProvider;
import com.trustify.trustify.enums.UserRole;
import com.trustify.trustify.enums.UserStatus;
import com.trustify.trustify.repository.Customer.UserRepository;
import com.trustify.trustify.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    @Transactional
    public User createOrUpdateUser(OAuth2UserInfo userInfo) {
        return userRepository.findByProviderAndProviderId(
                        userInfo.getProvider(),
                        userInfo.getProviderId()
                )
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo));
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setName(userInfo.getName());
        user.setAvatarUrl(userInfo.getAvatarUrl());
        user.setLastLoginAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Updated existing user: {}", savedUser.getEmail());
        return savedUser;
    }

    private User createNewUser(OAuth2UserInfo userInfo) {
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .status(UserStatus.ACTIVE)
                .name(userInfo.getName())
                .avatarUrl(userInfo.getAvatarUrl())
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .lastLoginAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created new user: {}", savedUser.getEmail());
        return savedUser;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
    public User getCurrentUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalStateException("No cookies in request");
        }

        String token = null;
        for (Cookie c : cookies) {
            if ("access_token".equals(c.getName())) {
                token = c.getValue();
                break;
            }
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("JWT token not found in cookies");
        }

        String email = jwtUtil.getEmailFromToken(token);
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Invalid JWT token");
        }

        User user = findByEmail(email);
        if (user == null) {
            throw new IllegalStateException("User not found for email: " + email);
        }
        return user;
    }

    public Page<User> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size));
    }

    public User updateUser(ReqUserDto reqUserDto) {
        User user = findByEmail(reqUserDto.getEmail());
        user.setName(reqUserDto.getName());
        user.setAvatarUrl(reqUserDto.getAvatarUrl());
        user.setCountry(reqUserDto.getCountry());
        return userRepository.save(user);
    }

    public User createUserAdmin(AdminDto adminDto) {
        User user = new User();
        user.setName(adminDto.getFullName());
        user.setEmail(adminDto.getEmail());
        user.setProvider(AuthProvider.LOCAL);
        user.setProviderId("admin");
        user.setPassword(passwordEncoder.encode(adminDto.getPassword())); // Hash password
        user.setRole(UserRole.ADMIN);
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            throw new IllegalStateException("Error creating admin user: " + e.getMessage());
        }
    }

    public User updateUserStatusByEmail(String email, UserStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        user.setStatus(status);
        return userRepository.save(user);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void validateUserCanModifyReview(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE users can create, update or delete reviews. Current status: " + user.getStatus());
        }
    }

}

