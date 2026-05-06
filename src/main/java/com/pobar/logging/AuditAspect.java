package com.pobar.logging;

import com.pobar.entity.AuditLog;
import com.pobar.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日誌 AOP：攔截標記 @Audit 的 Service 方法，寫入 audit_log。
 * 只記錄 action 和結果，不記錄方法參數（避免敏感資料外洩）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(com.pobar.logging.Audit)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Audit annotation = method.getAnnotation(Audit.class);

        String ip = getClientIp();
        Integer userId = null;
        String account = "anonymous";
        String role = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Integer) {
            userId = (Integer) auth.getPrincipal();
            account = auth.getName();
            role = auth.getDetails() != null ? auth.getDetails().toString() : null;
        }

        String result = "SUCCESS";
        String detail = null;

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            result = "FAIL";
            detail = e.getMessage();
            throw e;
        } finally {
            try {
                AuditLog auditLog = AuditLog.builder()
                        .userId(userId)
                        .account(account)
                        .role(role)
                        .action(annotation.action())
                        .entityType(annotation.entityType().isEmpty() ? null : annotation.entityType())
                        .result(result)
                        .detail(detail)
                        .ip(ip)
                        .createdAt(LocalDateTime.now())
                        .build();
                auditLogMapper.insert(auditLog);
            } catch (Exception ex) {
                // 日誌寫入失敗不應影響主流程
                log.error("[AUDIT] 寫入操作日誌失敗", ex);
            }
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
