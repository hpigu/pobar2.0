package com.pobar.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公開端點（客人端）
                .requestMatchers(HttpMethod.GET,  "/api/menu/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/attributes/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/tables/sessions/{token}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/orders/session/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/slots").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/cancel").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/availability").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/my").permitAll()
                // 顧客購物車（無需登入）
                .requestMatchers(HttpMethod.GET,    "/api/cart/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/cart/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/cart/**").permitAll()
                // 認證端點
                .requestMatchers("/api/auth/login").permitAll()
                // WebSocket
                .requestMatchers("/ws/**").permitAll()
                // 靜態資源（上傳的圖片）
                .requestMatchers("/uploads/**").permitAll()
                // 其餘需要登入
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
