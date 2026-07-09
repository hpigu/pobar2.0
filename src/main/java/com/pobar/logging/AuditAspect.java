package com.pobar.logging;

import com.pobar.entity.AuditLog;
import com.pobar.mapper.AuditLogMapper;
import com.pobar.security.AuthUser;
import com.pobar.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日誌 AOP：攔截 @Audit 方法。
 * 規則：
 *   1. 只記錄已登入（後台）操作；匿名請求一律不寫入。
 *   2. 詳細寫入 audit_log 各欄位：user_id / account / role / action /
 *      entity_type / entity_id / result / detail / ip / created_at。
 *   3. 透過 SpEL 由業務側決定 entity_id 與 detail，避免在 Aspect 端硬碰參數。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final int DETAIL_MAX_LENGTH = 1000;
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(com.pobar.logging.Audit)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Audit annotation = method.getAnnotation(Audit.class);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = null;
        String account = null;
        String role = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthUser au) {
            userId = au.id();
            account = au.account();
            role = au.role();
        }

        // 後台才記錄：未登入即不寫入（避免客戶端 API 灌爆 log）
        // 例外：LOGIN（驗證前為匿名但需記錄成敗）、以及標記 allowAnonymous 的公開端點（例如顧客自助取消訂位）。
        boolean isLoginAction = "LOGIN".equals(annotation.action());
        if (userId == null && !isLoginAction && !annotation.allowAnonymous()) {
            return joinPoint.proceed();
        }

        Object returnValue = null;
        Throwable thrown = null;
        try {
            returnValue = joinPoint.proceed();
            return returnValue;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            try {
                writeLog(joinPoint, method, annotation, userId, account, role, returnValue, thrown);
            } catch (Exception ex) {
                log.error("[AUDIT] 寫入操作日誌失敗 action={}", annotation.action(), ex);
            }
        }
    }

    private void writeLog(ProceedingJoinPoint joinPoint, Method method, Audit annotation,
                          Integer userId, String account, String role,
                          Object returnValue, Throwable thrown) {
        EvaluationContext ctx = buildContext(joinPoint, method, returnValue);

        String entityId = evalString(annotation.entityIdExpr(), ctx);
        String detail = buildDetail(annotation, ctx, thrown);

        // LOGIN：成功時補抓回傳的使用者資訊
        if (userId == null && returnValue != null) {
            try {
                Object id = returnValue.getClass().getMethod("getUserId").invoke(returnValue);
                if (id instanceof Integer ui) userId = ui;
                Object acc = returnValue.getClass().getMethod("getAccount").invoke(returnValue);
                if (acc instanceof String as) account = as;
                Object rl = returnValue.getClass().getMethod("getRole").invoke(returnValue);
                if (rl instanceof String rs) role = rs;
            } catch (Exception ignored) {
                // 不是 LoginResponse 就略過
            }
        }

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .account(account == null ? "anonymous" : account)
                .role(role)
                .action(annotation.action())
                .entityType(annotation.entityType().isEmpty() ? null : annotation.entityType())
                .entityId(entityId)
                .result(thrown == null ? "SUCCESS" : "FAIL")
                .detail(truncate(detail))
                .ip(getClientIp())
                .createdAt(LocalDateTime.now())
                .build();
        auditLogMapper.insert(auditLog);
    }

    private EvaluationContext buildContext(ProceedingJoinPoint joinPoint, Method method, Object returnValue) {
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, joinPoint.getArgs(), PARAM_DISCOVERER);
        ctx.setVariable("result", returnValue);
        return ctx;
    }

    private String evalString(String expr, EvaluationContext ctx) {
        if (expr == null || expr.isBlank()) return null;
        try {
            Expression e = PARSER.parseExpression(expr);
            Object v = e.getValue(ctx);
            return v == null ? null : v.toString();
        } catch (Exception ex) {
            log.warn("[AUDIT] SpEL 解析失敗 expr={} err={}", expr, ex.getMessage());
            return null;
        }
    }

    private String buildDetail(Audit annotation, EvaluationContext ctx, Throwable thrown) {
        String fromExpr = evalString(annotation.detailExpr(), ctx);
        if (thrown == null) return fromExpr;

        String err = thrown.getClass().getSimpleName() + ": " + thrown.getMessage();
        return fromExpr == null ? err : fromExpr + " | " + err;
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() <= DETAIL_MAX_LENGTH ? s : s.substring(0, DETAIL_MAX_LENGTH);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            return ClientIpResolver.resolve(request);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
