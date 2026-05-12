package com.pobar.dto.table;

import com.pobar.entity.TableSession;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TableSession 對外 Response（後台 / 員工用）— 不直接暴露 entity，便於未來增減欄位。
 */
@Data
public class TableSessionResponse {

    private Integer id;
    private String qrToken;
    private String status;
    private Integer partySize;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Integer openedById;

    public static TableSessionResponse from(TableSession s) {
        if (s == null) return null;
        TableSessionResponse r = new TableSessionResponse();
        r.id = s.getId();
        r.qrToken = s.getQrToken();
        r.status = s.getStatus();
        r.partySize = s.getPartySize();
        r.openedAt = s.getOpenedAt();
        r.closedAt = s.getClosedAt();
        r.openedById = s.getOpenedById();
        return r;
    }
}
