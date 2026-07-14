package com.pobar.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    /** 逗號分隔的允許 origin；空字串代表不啟用 CORS（走 nginx 反代同 origin） */
    @Value("${cors.allowed-origins:}")
    private String allowedOriginsRaw;

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
                .requestMatchers(HttpMethod.GET,  "/api/tables/sessions/{token}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/orders/session").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/slots").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/config").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations/cancel").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/availability").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/reservations/my").permitAll()
                // 顧客購物車（無需登入）
                .requestMatchers(HttpMethod.GET,    "/api/cart/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/cart/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/cart/**").permitAll()
                // Actuator：health / info 公開（含 liveness / readiness 子路徑供 k8s probe）
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                // 認證端點（login / refresh 公開，其餘需登入）
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/logout").authenticated()
                .requestMatchers("/api/auth/me").authenticated()
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
        List<String> origins = parseOrigins();
        // 同 origin 部署（走 nginx 反代）時 allowedOriginsRaw 為空 → CORS 形同關閉
        // 配多個 domain 時走白名單；用 `*` 開放全部（僅限 local 測試）
        if (origins.isEmpty()) {
            config.setAllowedOrigins(List.of()); // 空 list = 不放行任何 cross-origin 請求
        } else if (origins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        // 白名單模式才允許 credentials（避免 `*` + credentials 的安全漏洞）
        config.setAllowCredentials(!origins.isEmpty() && !origins.contains("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseOrigins() {
        if (allowedOriginsRaw == null || allowedOriginsRaw.isBlank()) return List.of();
        return List.of(allowedOriginsRaw.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
