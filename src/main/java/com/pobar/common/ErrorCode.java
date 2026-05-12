package com.pobar.common;

/**
 * 統一錯誤碼定義。前端依此 code 做對應處理。
 * <p>
 * 編碼規則：
 * <ul>
 *   <li>200       : 成功</li>
 *   <li>400-599   : HTTP 標準錯誤（保留通用語意）</li>
 *   <li>1000-1099 : 認證 / 授權</li>
 *   <li>1100-1199 : 使用者管理</li>
 *   <li>1200-1299 : 商品 / 菜單</li>
 *   <li>1300-1399 : 訂單</li>
 *   <li>1400-1499 : 桌位 / 座位</li>
 *   <li>1500-1599 : 預約</li>
 *   <li>1600-1699 : 支付 / 發票</li>
 *   <li>1700-1799 : 食材 / 庫存</li>
 *   <li>1800-1899 : 報表</li>
 *   <li>1900-1999 : 系統設定 / 備份</li>
 * </ul>
 * <p>
 * 對應前端文案請參見 {@code docs/error-codes.md}。
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ─────────────── 通用 / HTTP ───────────────
    public static final int SUCCESS              = 200;
    public static final int BAD_REQUEST          = 400;
    public static final int UNAUTHORIZED         = 401;
    public static final int FORBIDDEN            = 403;
    public static final int NOT_FOUND            = 404;
    public static final int METHOD_NOT_ALLOWED   = 405;
    public static final int CONFLICT             = 409;
    public static final int TOO_MANY_REQUESTS    = 429;
    public static final int INTERNAL_ERROR       = 500;

    // ─────────────── 認證 / 授權（1000-1099）───────────────
    public static final int INVALID_CREDENTIALS  = 1001;  // 帳號或密碼錯誤
    public static final int ACCOUNT_LOCKED       = 1002;  // 帳號被鎖
    public static final int IP_LOCKED            = 1003;  // 來源 IP 被鎖
    public static final int TOKEN_EXPIRED        = 1004;  // JWT 過期
    public static final int TOKEN_INVALID        = 1005;  // JWT 無效或被撤銷
    public static final int PASSWORD_MUST_CHANGE = 1006;  // 首次登入需強制改密碼
    public static final int ACCOUNT_INACTIVE     = 1007;  // 帳號被停用
    public static final int OLD_PASSWORD_WRONG   = 1008;  // 改密碼時舊密碼錯誤
    public static final int NEW_PASSWORD_WEAK    = 1009;  // 新密碼強度不足

    // ─────────────── 使用者（1100-1199）───────────────
    public static final int USER_NOT_FOUND       = 1101;
    public static final int USER_ACCOUNT_EXISTS  = 1102;
    public static final int USER_EMAIL_EXISTS    = 1103;
    public static final int CANNOT_DELETE_SELF   = 1104;
    public static final int CANNOT_MODIFY_OWN_ROLE = 1105;

    // ─────────────── 商品 / 菜單（1200-1299）───────────────
    public static final int PRODUCT_NOT_FOUND    = 1201;
    public static final int CATEGORY_NOT_FOUND   = 1202;
    public static final int RECIPE_NOT_FOUND     = 1203;
    public static final int PRODUCT_UNAVAILABLE  = 1204;
    public static final int CATEGORY_IN_USE      = 1205;  // 分類底下尚有商品，不可刪

    // ─────────────── 訂單（1300-1399）───────────────
    public static final int ORDER_NOT_FOUND      = 1301;
    public static final int ORDER_STATUS_INVALID = 1302;  // 狀態轉換非法
    public static final int CART_EMPTY           = 1303;
    public static final int CART_ITEM_NOT_FOUND  = 1304;

    // ─────────────── 桌位（1400-1499）───────────────
    public static final int TABLE_NOT_FOUND      = 1401;
    public static final int TABLE_OCCUPIED       = 1402;
    public static final int SESSION_NOT_FOUND    = 1403;
    public static final int SESSION_EXPIRED      = 1404;

    // ─────────────── 預約（1500-1599）───────────────
    public static final int RESERVATION_NOT_FOUND       = 1501;
    public static final int RESERVATION_SLOT_FULL      = 1502;
    public static final int RESERVATION_TIME_INVALID   = 1503;
    public static final int RESERVATION_ALREADY_CANCEL = 1504;

    // ─────────────── 支付 / 發票（1600-1699）───────────────
    public static final int PAYMENT_FAILED       = 1601;
    public static final int PAYMENT_NOT_FOUND    = 1602;
    public static final int INVOICE_FAILED       = 1603;
    public static final int ECPAY_NOT_CONFIGURED = 1604;
    public static final int AMOUNT_MISMATCH      = 1605;

    // ─────────────── 食材 / 庫存（1700-1799）───────────────
    public static final int INGREDIENT_NOT_FOUND = 1701;
    public static final int INGREDIENT_IN_USE    = 1702;
    public static final int STOCK_INSUFFICIENT   = 1703;

    // ─────────────── 報表（1800-1899）───────────────
    public static final int REPORT_RANGE_INVALID = 1801;
    public static final int REPORT_EXPORT_FAILED = 1802;

    // ─────────────── 系統 / 備份（1900-1999）───────────────
    public static final int SETTING_NOT_FOUND    = 1901;
    public static final int BACKUP_FAILED        = 1902;
    public static final int FILE_UPLOAD_FAILED   = 1903;
    public static final int FILE_TOO_LARGE       = 1904;
    public static final int FILE_TYPE_NOT_ALLOWED = 1905;
    public static final int STORAGE_NOT_CONFIGURED = 1906;
}
