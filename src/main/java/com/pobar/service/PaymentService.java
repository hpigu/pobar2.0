package com.pobar.service;

import com.pobar.dto.payment.CheckoutRequest;
import com.pobar.dto.payment.CheckoutResponse;
import com.pobar.dto.payment.PaymentPreviewResponse;

public interface PaymentService {

    PaymentPreviewResponse preview(Integer sessionId);

    CheckoutResponse checkout(Integer sessionId, CheckoutRequest request, Integer operatorId);
}
