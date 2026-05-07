package com.pobar.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderItemDisplay {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private String notes;
    private String type;
    private String status;
    private Integer sessionId;
    private String tableNames;
    private String ingredientNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
