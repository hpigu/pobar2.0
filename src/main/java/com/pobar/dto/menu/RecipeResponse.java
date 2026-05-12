package com.pobar.dto.menu;

import com.pobar.entity.Recipe;
import lombok.Data;

/**
 * Recipe 對外 Response — 只暴露最小必要欄位。
 * 詳細食材列表請用 RecipeDetailDto。
 */
@Data
public class RecipeResponse {

    private Integer id;
    private Integer productId;
    private String preparationNotes;

    public static RecipeResponse from(Recipe r) {
        if (r == null) return null;
        RecipeResponse resp = new RecipeResponse();
        resp.id = r.getId();
        resp.productId = r.getProductId();
        resp.preparationNotes = r.getPreparationNotes();
        return resp;
    }
}
