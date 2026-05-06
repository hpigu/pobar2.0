package com.pobar.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank
    private String paymentMethod;   // CASH, CARD, OTHER

    @Min(1)
    private Integer splitCount = 1;

    private String carrierType;     // MOBILE_BARCODE, CITIZEN_CERT, PAPER, null = no invoice
    private String carrierId;       // barcode value when carrierType != PAPER
}
