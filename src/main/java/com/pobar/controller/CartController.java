package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.order.CartItem;
import com.pobar.service.CartService;
import com.pobar.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final TableService tableService;

    @GetMapping("/{token}")
    public Result<List<CartItem>> getCart(@PathVariable String token) {
        tableService.getSessionByToken(token); // 驗證 token 有效
        return Result.ok(cartService.getCart(token));
    }

    @PostMapping("/{token}/items")
    public Result<List<CartItem>> addItem(@PathVariable String token,
                                           @RequestBody CartItem item) {
        tableService.getSessionByToken(token);
        return Result.ok(cartService.addItem(token, item));
    }

    @DeleteMapping("/{token}/items/{itemKey}")
    public Result<List<CartItem>> removeItem(@PathVariable String token,
                                              @PathVariable String itemKey) {
        tableService.getSessionByToken(token);
        return Result.ok(cartService.removeItem(token, itemKey));
    }
}
