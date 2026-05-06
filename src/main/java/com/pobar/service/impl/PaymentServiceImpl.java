package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pobar.dto.payment.CheckoutRequest;
import com.pobar.dto.payment.CheckoutResponse;
import com.pobar.dto.payment.PaymentPreviewResponse;
import com.pobar.entity.*;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.*;
import com.pobar.service.EcpayInvoiceService;
import com.pobar.service.PaymentService;
import com.pobar.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final TableSessionMapper sessionMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final PaymentMapper paymentMapper;
    private final InvoiceMapper invoiceMapper;
    private final SettingService settingService;
    private final EcpayInvoiceService ecpayInvoiceService;

    @Override
    public PaymentPreviewResponse preview(Integer sessionId) {
        TableSession session = requireOpenSession(sessionId);

        List<OrderItem> items = orderItemMapper.selectBySessionId(sessionId).stream()
                .filter(i -> !"CANCELLED".equals(i.getStatus()))
                .toList();

        return buildPreview(session, items);
    }

    @Override
    @Transactional
    @Audit(action = "CHECKOUT", entityType = "TableSession")
    public CheckoutResponse checkout(Integer sessionId, CheckoutRequest request, Integer operatorId) {
        requireOpenSession(sessionId);

        // 拒絕重複結帳
        Payment existing = paymentMapper.selectBySessionId(sessionId);
        if (existing != null) {
            throw new BusinessException(409, "此桌已結帳");
        }

        List<OrderItem> items = orderItemMapper.selectBySessionId(sessionId).stream()
                .filter(i -> !"CANCELLED".equals(i.getStatus()))
                .toList();

        if (items.isEmpty()) {
            throw new BusinessException(400, "此桌尚無消費品項");
        }

        PaymentPreviewResponse preview = buildPreview(null, items);

        int splitCount = request.getSplitCount() == null ? 1 : request.getSplitCount();
        BigDecimal amountPerPerson = preview.getTotal()
                .divide(BigDecimal.valueOf(splitCount), 0, RoundingMode.CEILING);

        // 建立付款記錄
        Payment payment = new Payment();
        payment.setSessionId(sessionId);
        payment.setSubtotal(preview.getSubtotal());
        payment.setServiceChargeRate(preview.getServiceChargeRate());
        payment.setServiceCharge(preview.getServiceCharge());
        payment.setTotal(preview.getTotal());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setSplitCount(splitCount);
        payment.setAmountPerPerson(amountPerPerson);
        payment.setProcessedBy(operatorId);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 關閉 session
        sessionMapper.update(null, new LambdaUpdateWrapper<TableSession>()
                .set(TableSession::getStatus, "CLOSED")
                .set(TableSession::getClosedAt, LocalDateTime.now())
                .eq(TableSession::getId, sessionId));

        // 開立發票（有傳 carrierType 才開）
        String invoiceNumber = null;
        if (request.getCarrierType() != null) {
            invoiceNumber = ecpayInvoiceService.issue(payment, request.getCarrierType(), request.getCarrierId());
            Invoice invoice = new Invoice();
            invoice.setPaymentId(payment.getId());
            invoice.setInvoiceNumber(invoiceNumber);
            invoice.setCarrierType(request.getCarrierType());
            invoice.setCarrierId(request.getCarrierId());
            invoice.setStatus("ISSUED");
            invoice.setIssuedAt(LocalDateTime.now());
            invoiceMapper.insert(invoice);
        }

        CheckoutResponse response = new CheckoutResponse();
        response.setPaymentId(payment.getId());
        response.setTotal(preview.getTotal());
        response.setAmountPerPerson(amountPerPerson);
        response.setSplitCount(splitCount);
        response.setInvoiceNumber(invoiceNumber);
        return response;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TableSession requireOpenSession(Integer sessionId) {
        TableSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"OPEN".equals(session.getStatus())) {
            throw new BusinessException(404, "找不到有效的桌位 session");
        }
        return session;
    }

    private PaymentPreviewResponse buildPreview(TableSession session, List<OrderItem> items) {
        BigDecimal serviceChargeRate = settingService.getDecimal(
                "service_charge_rate", new BigDecimal("0.10"));

        // 批次取得品項名稱，避免 N+1
        List<Integer> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<Integer, String> nameMap = productIds.isEmpty() ? Map.of() :
                productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Product::getName));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PaymentPreviewResponse.ItemSummary> summaries = new java.util.ArrayList<>();

        for (OrderItem item : items) {
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            PaymentPreviewResponse.ItemSummary s = new PaymentPreviewResponse.ItemSummary();
            s.setName(nameMap.getOrDefault(item.getProductId(), "未知品項"));
            s.setQuantity(item.getQuantity());
            s.setUnitPrice(item.getPrice());
            s.setLineTotal(lineTotal);
            s.setNote(item.getNotes());
            summaries.add(s);
        }

        BigDecimal serviceCharge = subtotal.multiply(serviceChargeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(serviceCharge);

        PaymentPreviewResponse resp = new PaymentPreviewResponse();
        if (session != null) resp.setSessionId(session.getId());
        resp.setSubtotal(subtotal);
        resp.setServiceChargeRate(serviceChargeRate);
        resp.setServiceCharge(serviceCharge);
        resp.setTotal(total);
        resp.setItems(summaries);
        return resp;
    }
}
