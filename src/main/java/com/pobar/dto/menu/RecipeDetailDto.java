package com.pobar.dto.menu;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RecipeDetailDto {
    private Integer id;
    private Integer productId;
    private String preparationNotes;
    private List<IngredientLine> ingredients;

    @Data
    public static class IngredientLine {
        private Integer ingredientId;
        private String ingredientName;
        private BigDecimal quantity;
        private String unit;
        private Integer displayOrder;
    }
}
