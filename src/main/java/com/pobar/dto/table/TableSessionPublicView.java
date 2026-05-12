package com.pobar.dto.table;

import com.pobar.entity.TableSession;
import lombok.Data;

/**
 * 客人掃 QR Code 後驗證 session 用，只回傳最小資訊。
 * 不暴露 openedById、openedAt 等內部欄位。
 */
@Data
public class TableSessionPublicView {

    private String status;       // OPEN / CLOSED
    private Integer partySize;

    public static TableSessionPublicView from(TableSession s) {
        if (s == null) return null;
        TableSessionPublicView v = new TableSessionPublicView();
        v.status = s.getStatus();
        v.partySize = s.getPartySize();
        return v;
    }
}
