package com.pobar.logging;

import java.lang.annotation.*;

/**
 * 標記後台操作方法，AOP 會寫入 audit_log。
 * 僅針對「已登入的後台操作」記錄；匿名（客戶端公開 API）不會留 log。
 *
 * SpEL 變數：
 *   #args[i]     — 方法參數（依索引）
 *   #p0, #p1...  — 同上，依位置
 *   參數名（若編譯時帶 -parameters）
 *   #result      — 方法回傳值（成功時可用）
 *
 * 範例：
 *   @Audit(action = "UPDATE_PRODUCT", entityType = "PRODUCT",
 *          entityIdExpr = "#id",
 *          detailExpr = "'name=' + #request.nameZh + ', price=' + #request.price")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    String action();

    String entityType() default "";

    /** SpEL：取得 entity_id（字串化後寫入）。 */
    String entityIdExpr() default "";

    /** SpEL：產生 detail 摘要。可為空。 */
    String detailExpr() default "";

    /** true 時即使匿名（未登入的公開端點）也會寫入 audit_log，例如顧客自助取消訂位。 */
    boolean allowAnonymous() default false;
}
