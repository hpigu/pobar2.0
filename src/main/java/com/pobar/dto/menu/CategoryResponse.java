package com.pobar.dto.menu;

import com.pobar.entity.Category;
import lombok.Data;

/**
 * Category 對外 Response — 不暴露 is_active 等內部欄位。
 */
@Data
public class CategoryResponse {

    private Integer id;
    private String nameZh;
    private String nameEn;
    private String type;
    private Integer displayOrder;

    public static CategoryResponse from(Category c) {
        if (c == null) return null;
        CategoryResponse r = new CategoryResponse();
        r.id = c.getId();
        r.nameZh = c.getNameZh();
        r.nameEn = c.getNameEn();
        r.type = c.getType();
        r.displayOrder = c.getDisplayOrder();
        return r;
    }
}
