package com.pobar.logging;

import java.lang.annotation.*;

/**
 * 標記需要寫入操作日誌的 Service 方法。
 * 範例：@Audit(action = "CREATE_PRODUCT", entityType = "PRODUCT")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    String action();

    String entityType() default "";
}
