package com.pobar.service;

import com.pobar.dto.order.SubmitOrderRequest;
import com.pobar.entity.OrderItem;

import java.util.List;

public interface OrderService {

    List<OrderItem> submit(String sessionToken, SubmitOrderRequest request);

    List<OrderItem> getBySession(String sessionToken);

    List<OrderItem> getBySessionId(Integer sessionId);

    List<OrderItem> getActiveByType(String type);

    OrderItem updateStatus(Integer itemId, String newStatus, String operatorRole);

    void cancelItem(Integer itemId, Integer cancelledByUserId);

    OrderItem updateItem(Integer itemId, String notes, Integer quantity, Integer operatorUserId);
}
