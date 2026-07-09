package com.pobar.service.impl;

import com.pobar.dto.order.OrderItemDisplay;
import com.pobar.dto.order.SubmitOrderRequest;
import com.pobar.entity.*;
import com.pobar.exception.BusinessException;
import com.pobar.mapper.OrderItemMapper;
import com.pobar.mapper.OrdersMapper;
import com.pobar.mapper.ProductMapper;
import com.pobar.service.CartService;
import com.pobar.service.OrderService;
import com.pobar.service.TableService;
import com.pobar.util.XssUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final TableService tableService;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CartService cartService;
    private final SimpMessagingTemplate messagingTemplate;

    // 每個 session 每分鐘最多送單 5 次，防止狂點
    private final Map<String, Bucket> orderRateLimiters = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public List<OrderItem> submit(String sessionToken, SubmitOrderRequest request) {
        TableSession session = tableService.getSessionByToken(sessionToken);

        // Rate limit 檢查
        Bucket bucket = orderRateLimiters.computeIfAbsent(sessionToken,
            k -> Bucket.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1)))
                .build());
        if (!bucket.tryConsume(1)) {
            throw new BusinessException(429, "點餐太頻繁，請稍後再試");
        }

        if (request.getItems().size() > 20) {
            throw new BusinessException("每次最多點 20 個品項");
        }

        Orders order = new Orders();
        order.setSessionId(session.getId());
        order.setCreatedAt(LocalDateTime.now());
        ordersMapper.insert(order);

        List<OrderItem> savedItems = request.getItems().stream().map(req -> {
            Product product = productMapper.selectById(req.getProductId());
            if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
                throw new BusinessException("品項不存在：" + req.getProductId());
            }
            if (!Boolean.TRUE.equals(product.getIsAvailable())) {
                throw new BusinessException("「" + product.getNameZh() + "」目前售完");
            }

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setQuantity(req.getQuantity());
            item.setPrice(product.getPrice());
            item.setNotes(XssUtil.sanitize(req.getNotes()));
            item.setType(product.getType());
            item.setStatus("PENDING");
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            orderItemMapper.insert(item);
            return item;
        }).toList();

        // 送出後清空該桌的購物車
        cartService.clearCart(sessionToken);

        // 廣播給廚房/吧台
        broadcastNewItems(savedItems);

        // 廣播給同桌客人更新訂單紀錄
        messagingTemplate.convertAndSend(
            "/topic/table/" + sessionToken + "/orders",
            getBySession(sessionToken)
        );

        return savedItems;
    }

    @Override
    public List<OrderItemDisplay> getBySession(String sessionToken) {
        TableSession session = tableService.getSessionByToken(sessionToken);
        return orderItemMapper.selectBySessionId(session.getId());
    }

    @Override
    @Transactional
    public OrderItem updateStatus(Integer itemId, String newStatus, String operatorRole) {
        OrderItem item = getItemOrThrow(itemId);

        validateStatusTransition(item, newStatus, operatorRole);

        item.setStatus(newStatus);
        item.setUpdatedAt(LocalDateTime.now());
        orderItemMapper.updateById(item);

        // 品項變為 READY → 通知服務生
        if ("READY".equals(newStatus)) {
            messagingTemplate.convertAndSend("/topic/staff/pickup", item);
        }

        // 廣播給廚房或吧台更新畫面
        broadcastByType(item.getType());

        return item;
    }

    @Override
    @Transactional
    public void cancelItem(Integer itemId, Integer cancelledByUserId) {
        OrderItem item = getItemOrThrow(itemId);
        if ("READY".equals(item.getStatus()) || "CANCELLED".equals(item.getStatus())) {
            throw new BusinessException("此品項已完成或已取消，無法取消");
        }
        item.setStatus("CANCELLED");
        item.setCancelledBy(cancelledByUserId);
        item.setCancelledAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        orderItemMapper.updateById(item);

        broadcastByType(item.getType());
    }

    @Override
    @Transactional
    public OrderItem updateItem(Integer itemId, String notes, Integer quantity, Integer operatorUserId) {
        OrderItem item = getItemOrThrow(itemId);
        if (!"PENDING".equals(item.getStatus())) {
            throw new BusinessException("已開始製作的品項無法修改");
        }
        if (notes != null) item.setNotes(XssUtil.sanitize(notes));
        if (quantity != null && quantity > 0) item.setQuantity(quantity);
        item.setUpdatedAt(LocalDateTime.now());
        orderItemMapper.updateById(item);
        return item;
    }

    private OrderItem getItemOrThrow(Integer itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(404, "品項不存在");
        return item;
    }

    private void validateStatusTransition(OrderItem item, String newStatus, String role) {
        String current = item.getStatus();

        // 只有對應角色能更新對應類型的品項
        if ("FOOD".equals(item.getType()) && !"KITCHEN".equals(role) && !"MANAGER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException("無權限更新食物訂單");
        }
        if ("DRINK".equals(item.getType()) && !"BARTENDER".equals(role) && !"MANAGER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException("無權限更新酒品訂單");
        }

        // 狀態流程：PENDING → READY 或 PENDING → IN_PROGRESS → READY
        boolean valid = ("PENDING".equals(current) && "IN_PROGRESS".equals(newStatus))
                     || ("PENDING".equals(current) && "READY".equals(newStatus))
                     || ("IN_PROGRESS".equals(current) && "READY".equals(newStatus));
        if (!valid) {
            throw new BusinessException("不合法的狀態變更：" + current + " → " + newStatus);
        }
    }

    private void broadcastNewItems(List<OrderItem> items) {
        boolean hasFood  = items.stream().anyMatch(i -> "FOOD".equals(i.getType()));
        boolean hasDrink = items.stream().anyMatch(i -> "DRINK".equals(i.getType()));

        if (hasFood)  broadcastByType("FOOD");
        if (hasDrink) broadcastByType("DRINK");
    }

    private void broadcastByType(String type) {
        if ("FOOD".equals(type)) {
            messagingTemplate.convertAndSend("/topic/kitchen",
                    orderItemMapper.selectActiveByType("FOOD"));
        } else {
            messagingTemplate.convertAndSend("/topic/bar",
                    orderItemMapper.selectActiveByType("DRINK"));
        }
    }

    @Override
    public List<OrderItemDisplay> getBySessionId(Integer sessionId) {
        return orderItemMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<OrderItemDisplay> getActiveByType(String type) {
        return orderItemMapper.selectActiveByType(type);
    }
}
