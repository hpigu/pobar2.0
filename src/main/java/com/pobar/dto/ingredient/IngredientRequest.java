package com.pobar.dto.ingredient;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IngredientRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String unit;

    private String category;

    private Boolean isAvailable;
}
