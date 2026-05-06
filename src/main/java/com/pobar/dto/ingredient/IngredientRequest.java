package com.pobar.dto.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IngredientRequest {

    @NotBlank
    private String name;
    private String unit;

    @NotNull
    private BigDecimal quantity;

    private BigDecimal lowStockThreshold;
    private Boolean isAvailable;
}
