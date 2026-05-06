package com.pobar.dto.payment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutResponse {

    private Integer paymentId;
    private BigDecimal total;
    private BigDecimal amountPerPerson;
    private Integer splitCount;
    private String invoiceNumber;   // null if no invoice issued
}
