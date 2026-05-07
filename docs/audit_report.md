# POBAR 2.0 前後端對應與資料庫使用情況 審計報告

產生日期：2026-05-07
範圍：F:\project\pobar2.0

## 1. 編譯/建置狀況

| 項目 | 結果 |
| --- | --- |
| 後端 `mvn compile` | ✅ 通過 |
| 前端 `npm run build` | ✅ 通過（697ms 內完成） |

### 1.1 本次修復的編譯錯誤
| 檔案 | 問題 | 修復 |
| --- | --- | --- |
| `src/main/java/com/pobar/service/impl/PaymentServiceImpl.java` | `selectBySessionId` 已改回傳 `List<OrderItemDisplay>`，但程式仍宣告 `List<OrderItem>` 造成型別不相容（行 42、61） | 將 `items` 型別改為 `OrderItemDisplay`；buildPreview 直接讀 `productName`，移除 `ProductMapper` 名稱對映（已不需要 N+1 防呆） |

> 原 commit `ca41b4e`、`6e9103f` 移除 drink_attribute 同時調整 OrderItemMapper 回傳型別，但結帳服務漏改。

## 2. 前後端欄位對應審計

對全部 10 個前端頁面逐一檢視「呼叫端點 → 控制器 → DTO/Entity → Mapper SQL → schema 欄位」鏈路。

| 頁面 | 呼叫的 API | 欄位對應 | 狀態 |
| --- | --- | --- | --- |
| `BarDisplayPage.vue` | GET `/api/orders/display?type=DRINK`, PUT `/api/orders/items/{id}/status` | OrderItemDisplay 全部欄位（id/status/tableNames/productName/quantity/ingredientNames/notes/createdAt/sessionId）皆有對應 | ✅ |
| `KitchenDisplayPage.vue` | 同上 type=FOOD | 同上 | ✅ |
| `CustomerOrderPage.vue` | GET `/api/tables/sessions/{token}` `/api/menu` `/api/categories` `/api/cart/{token}` `/api/menu/{id}/ingredients`；POST `/api/orders` | Product/Category/CartItem 欄位齊全，POST body 與 SubmitOrderRequest 相符 | ✅ |
| `StaffPage.vue` | `/api/tables`, `/api/orders/session/{id}`, `/api/reservations`, `/api/tables/sessions`, `/api/sessions/{id}/payment(/preview)`, `/api/reservations/{id}/status` | BarTableVO、ReservationResponse、PaymentPreviewResponse、CheckoutRequest 欄位齊全 | ✅ |
| `admin/MenuPage.vue` | `/api/categories`, `/api/menu`, `/api/menu/{id}/recipe-detail`, `/api/menu/{id}/recipe`, `/api/ingredients` | Product 含 availableFrom/availableTo；RecipeDetailDto 結構相符；ProductSaveRequest 接受全部 | ✅ |
| `admin/TablesPage.vue` | `/api/tables` CRUD | BarTable 欄位齊全 | ✅ |
| `admin/ReportsPage.vue` | `/api/reports/{daily|ranking|monthly}` | 三個 Response DTO 欄位齊全 | ✅ |
| `admin/AttributesPage.vue` | （無 API） | 已是「屬性管理功能已移除」存根 | ✅ |
| `LoginPage.vue` | `/api/auth/login` | LoginRequest/Response 欄位齊全 | ✅ |
| `ReservationPage.vue` | `/api/reservations/my`, `/slots`, POST `/api/reservations` | ReservationRequest/Response、TimeSlotResponse 欄位齊全 | ✅ |

**結論：未發現欄位層級的缺漏或型別不符。**

## 3. 資料庫使用情況

### 3.1 未使用的資料表
| 資料表 | 位置 | 建議 |
| --- | --- | --- |
| `promotion` | `sql/schema.sql` 第 248–262 行 | 沒有任何 Entity/Mapper/Controller/前端呼叫；若無短期計畫可刪除 |

### 3.2 未使用的 Mapper / 程式檔
| 檔案 | 說明 | 建議 |
| --- | --- | --- |
| `src/main/java/com/pobar/mapper/ProductAttributeMapper.java` | 引用已不存在的 `product_attribute` 表/`attribute_option_id` 欄位；服務層、控制器、前端皆無使用 | 刪除 |

### 3.3 SQL 種子檔
| 檔案 | 與 schema 對應 |
| --- | --- |
| `sql/seed_cocktails.sql` | ✅ 全部欄位（category_id、name_zh、name_en、type、display_order、is_active、price、created_by）皆與目前 schema 一致 |
| `sql/seed_ingredients.sql` | ✅ 與 ingredient schema 一致 |

### 3.4 Mapper 查詢驗證
- `OrderItemMapper.selectActiveByType` / `selectBySessionId` / `selectSalesRanking`：欄位皆存在；`tableNames`、`ingredientNames` 為 GROUP_CONCAT 衍生欄，已映射至 OrderItemDisplay。
- `BarTableMapper.listWithStatus` LEFT JOIN 邏輯正確。
- `ProductMapper.xml`：欄位有效。

### 3.5 殘留 worktree（非主程式碼）
`.claude/worktrees/modest-brahmagupta-2b3869/` 內仍有 `DrinkAttributeType.java`、`DrinkAttributeOption.java`、相關 Mapper。這些屬於暫存分支不影響主程式碼，可由 git 自動清理。

## 4. 行動清單

| 優先 | 動作 |
| --- | --- |
| ✅ 已完成 | 修復 PaymentServiceImpl 型別錯誤，後端編譯通過 |
| 建議 | 刪除 `ProductAttributeMapper.java` |
| 建議 | 刪除 schema.sql 中 `promotion` 表（若無計畫使用） |
| 可選 | 考慮從前端路由與側欄移除 AttributesPage 入口，目前是空殼 |

