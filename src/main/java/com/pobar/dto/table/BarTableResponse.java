package com.pobar.dto.table;

import com.pobar.entity.BarTable;
import lombok.Data;

import java.math.BigDecimal;

/**
 * BarTable 對外 Response — save/update 後使用。
 * 列表查詢請用 {@link BarTableVO}（含 session JOIN 後的狀態資訊）。
 */
@Data
public class BarTableResponse {

    private Integer id;
    private String name;
    private String type;
    private Integer capacity;
    private BigDecimal posX;
    private BigDecimal posY;
    private Boolean isLocked;

    public static BarTableResponse from(BarTable t) {
        if (t == null) return null;
        BarTableResponse r = new BarTableResponse();
        r.id = t.getId();
        r.name = t.getName();
        r.type = t.getType();
        r.capacity = t.getCapacity();
        r.posX = t.getPosX();
        r.posY = t.getPosY();
        r.isLocked = t.getIsLocked();
        return r;
    }
}
