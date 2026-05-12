package com.pobar.config;

import com.pobar.entity.User;
import com.pobar.mapper.JwtBlacklistMapper;
import com.pobar.security.JwtUtil;
import com.pobar.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket / STOMP 設定。
 *
 * 鑑權策略：
 *   - CONNECT 允許匿名（顧客 QR 頁面用），有帶 JWT 會建立認證
 *   - SUBSCRIBE 階段依 destination 檢查：
 *       /topic/table/{token}/*  → 用 session token 驗證
 *       /topic/staff/**、/topic/kitchen、/topic/bar、/topic/tables → 必須有 JWT
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistMapper jwtBlacklistMapper;
    private final TableService tableService;

    public WebSocketConfig(JwtUtil jwtUtil,
                           JwtBlacklistMapper jwtBlacklistMapper,
                           @Lazy TableService tableService) {
        this.jwtUtil = jwtUtil;
        this.jwtBlacklistMapper = jwtBlacklistMapper;
        this.tableService = tableService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // CONNECT 階段：有帶 JWT 就建立認證；沒有也放行（顧客匿名 OK）
                    String auth = accessor.getFirstNativeHeader("Authorization");
                    String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
                    if (token != null && jwtUtil.isValid(token)
                            && !jwtBlacklistMapper.existsByTokenHash(User.hashToken(token))
                            && !jwtUtil.mustChangePassword(token)) {
                        int userId = jwtUtil.getUserId(token);
                        String role = jwtUtil.getRole(token);
                        UsernamePasswordAuthenticationToken springAuth =
                                new UsernamePasswordAuthenticationToken(
                                        userId, null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        springAuth.setDetails(role);
                        accessor.setUser(springAuth);
                    }
                }
                else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String dest = accessor.getDestination();
                    if (dest == null) return message;

                    if (isStaffTopic(dest)) {
                        if (accessor.getUser() == null) {
                            log.warn("[WS] SUBSCRIBE 拒絕（staff topic 無認證）: {}", dest);
                            throw new IllegalArgumentException("此 topic 需登入");
                        }
                    } else if (dest.startsWith("/topic/table/")) {
                        String sessionToken = extractSessionToken(dest);
                        try {
                            tableService.getSessionByToken(sessionToken);
                        } catch (Exception e) {
                            log.warn("[WS] SUBSCRIBE 拒絕（session token 無效）: {}", dest);
                            throw new IllegalArgumentException("session token 無效");
                        }
                    } else {
                        // 其他 /topic/* 都需要認證
                        if (accessor.getUser() == null) {
                            log.warn("[WS] SUBSCRIBE 拒絕（未知 topic 且未認證）: {}", dest);
                            throw new IllegalArgumentException("此 topic 需登入");
                        }
                    }
                }
                return message;
            }
        });
    }

    private boolean isStaffTopic(String dest) {
        return dest.startsWith("/topic/staff")
                || dest.startsWith("/topic/kitchen")
                || dest.startsWith("/topic/bar")
                || dest.startsWith("/topic/tables");
    }

    /** /topic/table/{token}/cart → 取 {token} */
    private String extractSessionToken(String dest) {
        String prefix = "/topic/table/";
        if (!dest.startsWith(prefix)) return null;
        String rest = dest.substring(prefix.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
