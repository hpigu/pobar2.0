package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.order.CartItem;
import com.pobar.exception.BusinessException;
import com.pobar.service.CartService;
import com.pobar.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 顧客端購物車。
 * Session token 改透過 {@code X-Session-Token} HTTP header 傳遞，
 * 不再放在 URL path / query 內，避免被寫進 nginx access log。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final TableService tableService;

    @GetMapping
    public Result<List<CartItem>> getCart(@RequestHeader("X-Session-Token") String token) {
        validateToken(token);
        return Result.ok(cartService.getCart(token));
    }

    @PostMapping("/items")
    public Result<List<CartItem>> addItem(@RequestHeader("X-Session-Token") String token,
                                           @RequestBody CartItem item) {
        validateToken(token);
        return Result.ok(cartService.addItem(token, item));
    }

    @DeleteMapping("/items/{itemKey}")
    public Result<List<CartItem>> removeItem(@RequestHeader("X-Session-Token") String token,
                                              @PathVariable String itemKey) {
        validateToken(token);
        return Result.ok(cartService.removeItem(token, itemKey));
    }

    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "缺少 X-Session-Token");
        }
        tableService.getSessionByToken(token); // 驗證 token 有效
    }
}
