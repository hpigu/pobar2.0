package com.pobar.dto.menu;

import com.pobar.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product 對外 Response — 不暴露 createdBy、createdAt、updatedAt 等內部欄位。
 * 客人端 / 後台共用此 DTO。
 */
@Data
public class ProductResponse {

    private Integer id;
    private Integer categoryId;
    private String nameZh;
    private String nameEn;
    private BigDecimal price;
    private String type;
    private String imageUrl;
    private Boolean isAvailable;
    /** 後台才會看到此欄位（前端忽略即可） */
    private Boolean isActive;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    /** 公開：酒譜食材名稱（依 display_order 排序）；若無酒譜則為空 list */
    private List<String> ingredients;

    public static ProductResponse from(Product p) {
        if (p == null) return null;
        ProductResponse r = new ProductResponse();
        r.id = p.getId();
        r.categoryId = p.getCategoryId();
        r.nameZh = p.getNameZh();
        r.nameEn = p.getNameEn();
        r.price = p.getPrice();
        r.type = p.getType();
        r.imageUrl = p.getImageUrl();
        r.isAvailable = p.getIsAvailable();
        r.isActive = p.getIsActive();
        r.availableFrom = p.getAvailableFrom();
        r.availableTo = p.getAvailableTo();
        return r;
    }
}
