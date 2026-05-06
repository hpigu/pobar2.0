package com.pobar.dto.ingredient;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IngredientResponse {

    private Integer id;
    private String name;
    private String unit;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
}
