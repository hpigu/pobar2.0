package com.pobar.dto.menu;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecipeSaveRequest {

    private String preparationNotes;

    @NotEmpty(message = "酒譜至少需要一個食材")
    private List<IngredientItem> ingredients;

    @Data
    public static class IngredientItem {
        private Integer ingredientId;
        private BigDecimal quantity;
        private String unit;
        private Integer displayOrder;
    }
}
