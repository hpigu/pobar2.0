package com.pobar.service;

import com.pobar.entity.Payment;

public interface EcpayInvoiceService {

    /**
     * 向 ECPay 開立電子發票，回傳發票號碼。
     * carrierType: MOBILE_BARCODE / CITIZEN_CERT / PAPER
     * carrierId: 手機條碼或自然人憑證號碼，PAPER 時為 null
     */
    String issue(Payment payment, String carrierType, String carrierId);
}
