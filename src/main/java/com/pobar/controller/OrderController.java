package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.order.OrderItemDisplay;
import com.pobar.dto.order.SubmitOrderRequest;
import com.pobar.entity.OrderItem;
import com.pobar.security.AuthUser;
import com.pobar.service.OrderService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 客人送出訂單（公開，以 X-Session-Token header 識別桌子）
    @PostMapping("/orders")
    public Result<List<OrderItem>> submit(@RequestHeader("X-Session-Token") String token,
                                           @Valid @RequestBody SubmitOrderRequest request) {
        return Result.ok(orderService.submit(token, request));
    }

    // 客人查看本桌訂單（公開，X-Session-Token header）
    @GetMapping("/orders/session")
    public Result<List<OrderItemDisplay>> getBySession(@RequestHeader("X-Session-Token") String token) {
        return Result.ok(orderService.getBySession(token));
    }

    // 服務生查看指定 session 的訂單（by sessionId）
    @GetMapping("/orders/session/{sessionId}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<List<OrderItemDisplay>> getBySessionId(@PathVariable Integer sessionId) {
        return Result.ok(orderService.getBySessionId(sessionId));
    }

    // 廚房/吧台取得待處理品項（依類型）
    @GetMapping("/orders/display")
    @PreAuthorize("hasAnyRole('KITCHEN','BARTENDER','MANAGER','ADMIN')")
    public Result<List<OrderItemDisplay>> display(@RequestParam String type) {
        return Result.ok(orderService.getActiveByType(type.toUpperCase()));
    }

    // 廚師/調酒師更新品項狀態（支援 PUT 和 PATCH）
    @PutMapping("/orders/items/{itemId}/status")
    @PatchMapping("/orders/items/{itemId}/status")
    @PreAuthorize("hasAnyRole('KITCHEN','BARTENDER','MANAGER','ADMIN')")
    public Result<OrderItem> updateStatus(@PathVariable Integer itemId,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        String role = ((AuthUser) auth.getPrincipal()).role();
        return Result.ok(orderService.updateStatus(itemId, body.get("status"), role));
    }

    // 服務生取消品項
    @DeleteMapping("/orders/items/{itemId}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<?> cancelItem(@PathVariable Integer itemId, Authentication auth) {
        Integer userId = ((AuthUser) auth.getPrincipal()).id();
        orderService.cancelItem(itemId, userId);
        return Result.ok();
    }

    // 服務生修改品項（備註 / 數量，只限 PENDING 狀態）
    @PutMapping("/orders/items/{itemId}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<OrderItem> updateItem(@PathVariable Integer itemId,
                                         @RequestBody UpdateItemRequest request,
                                         Authentication auth) {
        Integer userId = ((AuthUser) auth.getPrincipal()).id();
        return Result.ok(orderService.updateItem(itemId, request.getNotes(), request.getQuantity(), userId));
    }

    @Data
    static class UpdateItemRequest {
        private String notes;
        private Integer quantity;
    }
}
