package com.pobar.service;

import com.pobar.dto.order.CartItem;

import java.util.List;

public interface CartService {

    List<CartItem> addItem(String sessionToken, CartItem item);

    List<CartItem> removeItem(String sessionToken, String itemKey);

    List<CartItem> getCart(String sessionToken);

    void clearCart(String sessionToken);
}
