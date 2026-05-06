package com.pobar.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItem {
    private String key;         // 前端產生的唯一識別，用於移除單筆
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private String notes;
    private String type;        // FOOD, DRINK
}
