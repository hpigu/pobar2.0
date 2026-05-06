package com.pobar.service.impl;

import com.pobar.dto.order.CartItem;
import com.pobar.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 購物車存在記憶體中（單機夠用）。
 * 每次異動都透過 WebSocket 廣播給同桌所有裝置，實現即時同步。
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    // sessionToken → 購物車品項清單
    private final Map<String, List<CartItem>> carts = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<CartItem> addItem(String sessionToken, CartItem item) {
        List<CartItem> cart = carts.computeIfAbsent(sessionToken, k -> new ArrayList<>());
        synchronized (cart) {
            cart.add(item);
        }
        broadcast(sessionToken, cart);
        return cart;
    }

    @Override
    public List<CartItem> removeItem(String sessionToken, String itemKey) {
        List<CartItem> cart = carts.getOrDefault(sessionToken, new ArrayList<>());
        synchronized (cart) {
            cart.removeIf(i -> itemKey.equals(i.getKey()));
        }
        broadcast(sessionToken, cart);
        return cart;
    }

    @Override
    public List<CartItem> getCart(String sessionToken) {
        return carts.getOrDefault(sessionToken, new ArrayList<>());
    }

    @Override
    public void clearCart(String sessionToken) {
        carts.remove(sessionToken);
        broadcast(sessionToken, new ArrayList<>());
    }

    private void broadcast(String sessionToken, List<CartItem> cart) {
        messagingTemplate.convertAndSend("/topic/table/" + sessionToken + "/cart", cart);
    }
}
