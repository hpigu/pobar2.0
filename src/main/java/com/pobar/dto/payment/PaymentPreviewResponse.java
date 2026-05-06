package com.pobar.dto.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PaymentPreviewResponse {

    private Integer sessionId;
    private String tableNames;
    private BigDecimal subtotal;
    private BigDecimal serviceChargeRate;
    private BigDecimal serviceCharge;
    private BigDecimal total;
    private List<ItemSummary> items;

    @Data
    public static class ItemSummary {
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String note;
    }
}
