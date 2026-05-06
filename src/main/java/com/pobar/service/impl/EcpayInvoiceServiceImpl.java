package com.pobar.service.impl;

import com.pobar.entity.Payment;
import com.pobar.service.EcpayInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ECPay 電子發票 API 串接。
 * 目前為 stub 實作，實際上線前需填入 MerchantID、HashKey、HashIV 並呼叫 ECPay API。
 * API 文件: https://developers.ecpay.com.tw/?p=7256
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcpayInvoiceServiceImpl implements EcpayInvoiceService {

    @Value("${ecpay.merchant-id:#{null}}")
    private String merchantId;

    @Value("${ecpay.hash-key:#{null}}")
    private String hashKey;

    @Value("${ecpay.hash-iv:#{null}}")
    private String hashIv;

    @Value("${ecpay.invoice.api-url:https://einvoice-stage.ecpay.com.tw/B2CInvoice/Issue}")
    private String apiUrl;

    @Override
    public String issue(Payment payment, String carrierType, String carrierId) {
        if (merchantId == null || hashKey == null || hashIv == null) {
            // ECPay 未設定時回傳模擬發票號碼（開發/測試用）
            String fakeNumber = "AB" + String.format("%08d", payment.getId());
            log.warn("ECPay 未設定，回傳模擬發票號碼: {}", fakeNumber);
            return fakeNumber;
        }

        // TODO: 實作 ECPay 電子發票 API 呼叫
        // 步驟：
        // 1. 組裝 params (MerchantID, RelateNumber, CustomerEmail, Carrier*, SalesAmount...)
        // 2. URL encode 後 SHA256 加密產生 CheckMacValue
        // 3. POST 到 apiUrl
        // 4. 解析回應取得 InvoiceNumber
        // 參考: https://developers.ecpay.com.tw/?p=7256

        throw new UnsupportedOperationException("ECPay 發票串接尚未實作，請設定 ecpay.* 並完成 API 呼叫");
    }
}
