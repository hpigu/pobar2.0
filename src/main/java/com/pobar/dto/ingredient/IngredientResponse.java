package com.pobar.dto.ingredient;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IngredientResponse {

    private Integer id;
    private String name;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal lowStockThreshold;
    private Boolean isAvailable;
    private Boolean isLow;
    private LocalDateTime updatedAt;
}
