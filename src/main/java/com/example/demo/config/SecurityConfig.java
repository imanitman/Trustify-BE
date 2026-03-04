// java
package com.trustify.trustify.config;

import com.trustify.trustify.dto.Req.OAuth2UserInfo;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.UserRole;
import com.trustify.trustify.filter.JwtAuthenticationFilter;
import com.trustify.trustify.service.OAuth2Service;
import com.trustify.trustify.service.RedisService;
import com.trustify.trustify.service.Customer.UserService;
import com.trustify.trustify.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;
import java.util.UUID;

@EnableMethodSecurity
@Configuration
@AllArgsConstructor
@Slf4j
public class SecurityConfig {

    private final OAuth2Service oAuth2Service;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PremiumInterceptor premiumInterceptor;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // nếu là API, thường disable CSRF; tuỳ nhu cầu
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**","/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/companies/timeSubscription/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .requestMatchers("/api/v1/ai/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .requestMatchers("/admin/company/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                .successHandler(customOAuth2SuccessHandler())

        );

        return http.build();
    }



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://headlong-noncongregative-chantell.ngrok-free.dev",
                "http://localhost:3000",
                "https://demo-trust.vercel.app",
                "https://naisu.vercel.app",
                "https://naisu-tau.vercel.app",
                "https://trustify-gux53e1bo-vwthu-22s-projects.vercel.app",
                "https://trustify-pied.vercel.app",
                "https://trustify.io.vn",
                "https://trustify-company.vercel.app",
                "https://trustify-admin.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler customOAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            try {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                log.info("OAuth2 callback received. Attributes: {}", oauth2User.getAttributes());

                String requestUri = request.getRequestURI();
                String registrationId = requestUri.contains("google") ? "google" : "facebook";
                log.info("Registration ID: {}, Request URI: {}", registrationId, requestUri);

                OAuth2UserInfo userInfo = oAuth2Service.extractUserInfo(oauth2User, registrationId);
                log.info("Extracted user info - Email: {}, Name: {}", userInfo.getEmail(), userInfo.getName());

                // Sửa logic kiểm tra user
                User user;
                try {
                    user = userService.findByEmail(userInfo.getEmail());
                    log.info("User found with ID: {}", user.getId());
                } catch (UsernameNotFoundException e) {
                    log.info("User not found, creating new user");
                    user = userService.createOrUpdateUser(userInfo);
                }

                String token = jwtUtil.generateToken(user, UserRole.USER );
                log.info("JWT token generated successfully");

                String stateCode = UUID.randomUUID().toString();
                redisService.saveStateCode(stateCode, token);
                log.info("State code saved to Redis: {}", stateCode);

                String redirectUrl = "https://trustify-pied.vercel.app/auth/callback?state=" + stateCode;
                log.info("Redirecting to: {}", redirectUrl);
                response.sendRedirect(redirectUrl);

            } catch (Exception e) {
                log.error("OAuth2 authentication failed", e);
                log.error("Error message: {}", e.getMessage());
                log.error("Error class: {}", e.getClass().getName());
                try {
                    response.sendRedirect("https://trustify-pied.vercel.app/login?error=oauth2_failed");
                } catch (Exception redirectError) {
                    log.error("Failed to redirect to error page", redirectError);
                }
            }
        };
    }


}
