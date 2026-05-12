package com.pobar.exception;

import com.pobar.common.ErrorCode;
import com.pobar.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 統一例外處理。
 * 規則：
 *   1. 不洩漏 stacktrace 給前端（500 時也只回通用訊息）。
 *   2. 業務錯誤一律 HTTP 400 + 業務 code 在 body 內。
 *   3. 5xx 才會印 ERROR log，4xx 用 WARN。
 *   4. 加上 request path 方便追查。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────── 業務例外 ───────────────────

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("[{}] {} BusinessException code={} msg={}",
                req.getMethod(), req.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ─────────────────── 驗證 / 參數錯誤 ───────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(ErrorCode.BAD_REQUEST, message.isEmpty() ? "參數驗證失敗" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraint(ConstraintViolationException e) {
        return Result.fail(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ErrorCode.BAD_REQUEST, "缺少必要參數：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(ErrorCode.BAD_REQUEST, "參數格式錯誤：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ErrorCode.BAD_REQUEST, "請求格式錯誤（JSON 解析失敗）");
    }

    // ─────────────────── HTTP 方法 / 路徑 ───────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ErrorCode.METHOD_NOT_ALLOWED, "不支援的 HTTP 方法：" + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<?> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return Result.fail(ErrorCode.BAD_REQUEST, "不支援的 Content-Type");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandler(NoHandlerFoundException e) {
        return Result.fail(ErrorCode.NOT_FOUND, "找不到 API 端點");
    }

    // ─────────────────── 認證 / 授權 ───────────────────

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuth(AuthenticationException e) {
        return Result.fail(ErrorCode.UNAUTHORIZED, "未登入或登入已失效");
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleCredentialsNotFound(AuthenticationCredentialsNotFoundException e) {
        return Result.fail(ErrorCode.UNAUTHORIZED, "未登入");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.fail(ErrorCode.FORBIDDEN, "權限不足");
    }

    // ─────────────────── 檔案上傳 ───────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<?> handleUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(ErrorCode.FILE_TOO_LARGE, "檔案超過大小限制");
    }

    // ─────────────────── 資料層 ───────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<?> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("[{}] {} DataIntegrityViolation: {}", req.getMethod(), req.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return Result.fail(ErrorCode.CONFLICT, "資料衝突（可能為重複鍵或外鍵約束）");
    }

    // ─────────────────── 兜底 ───────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleGeneral(Exception e, HttpServletRequest req) {
        log.error("[{}] {} Unhandled exception", req.getMethod(), req.getRequestURI(), e);
        // 絕不外洩 stacktrace 或內部訊息
        return Result.fail(ErrorCode.INTERNAL_ERROR, "系統錯誤，請聯絡管理員");
    }
}
