# Pobar 酒吧系統 — API 一覽

> 本文對照實作（`src/main/java/com/pobar/controller/`）整理，非僅規劃。
> 角色標記 `WAITER+` = WAITER / MANAGER / ADMIN；`Public（帶 token）` = 免登入但需帶 `X-Session-Token`（掃碼取得的 QR token）。
> 所有回應統一為 `{ code, message, data }`，錯誤碼見 [error-codes.md](error-codes.md)。

## 認證

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| POST | `/api/auth/login` | 員工登入，回傳 access + refresh token | Public |
| POST | `/api/auth/refresh` | 以 refresh token 換發新 token（輪替） | Public |
| POST | `/api/auth/logout` | 登出（access token 加入黑名單） | 已登入 |
| POST | `/api/auth/change-password` | 修改密碼（首次登入強制改密走同一支） | 已登入 |
| GET | `/api/auth/me` | 取得目前登入者資訊 | 已登入 |

---

## 桌位

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/tables` | 取得所有桌位（含狀態與目前 session） | WAITER+ |
| POST | `/api/tables` | 新增桌位 | MANAGER ADMIN |
| PUT | `/api/tables/{id}` | 編輯桌位（名稱、容量、座標、鎖定） | MANAGER ADMIN |
| DELETE | `/api/tables/{id}` | 刪除桌位 | MANAGER ADMIN |
| POST | `/api/tables/sessions` | 開桌（單桌或多桌），回傳 session + QR token | WAITER+ |
| DELETE | `/api/tables/sessions/{sessionId}` | 關桌 | WAITER+ |
| POST | `/api/tables/sessions/{sessionId}/merge` | 併桌（追加 table 到 session，前端尚無入口） | WAITER+ |
| GET | `/api/tables/sessions/{token}` | 以 QR token 查 session（客人掃碼後呼叫） | Public |

---

## 購物車（客人端，in-memory）

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/cart` | 取得本桌購物車 | Public（帶 token） |
| POST | `/api/cart/items` | 加入品項（含備註） | Public（帶 token） |
| DELETE | `/api/cart/items/{itemKey}` | 移除品項 | Public（帶 token） |

---

## 點餐

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| POST | `/api/orders` | 送出訂單（一次多個品項） | Public（帶 token） |
| GET | `/api/orders/session` | 查本桌訂單（依 token） | Public（帶 token） |
| GET | `/api/orders/session/{sessionId}` | 查指定 session 訂單 | WAITER+ |
| GET | `/api/orders/display?type=FOOD\|DRINK` | 出餐看板清單 | KITCHEN BARTENDER MANAGER ADMIN |
| PUT / PATCH | `/api/orders/items/{itemId}/status` | 更新品項狀態（IN_PROGRESS / READY） | KITCHEN BARTENDER MANAGER ADMIN |
| DELETE | `/api/orders/items/{itemId}` | 取消品項 | WAITER+ |
| PUT | `/api/orders/items/{itemId}` | 修改品項備註、數量 | WAITER+ |

---

## 菜單 / 酒單

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/menu` | 取得所有上架品項，支援篩選參數 | Public |
| GET | `/api/menu/{id}` | 取得單一品項詳細資訊 | Public |
| POST | `/api/menu` | 新增品項 | MANAGER ADMIN |
| PUT | `/api/menu/{id}` | 編輯品項 | MANAGER ADMIN |
| DELETE | `/api/menu/{id}` | 下架品項（軟刪除） | MANAGER ADMIN |
| PUT | `/api/menu/{id}/availability` | 切換臨時售完 | MANAGER ADMIN |
| POST | `/api/menu/{id}/image` | 上傳品項照片（含內容與尺寸驗證） | MANAGER ADMIN |
| GET | `/api/categories` | 取得分類清單 | Public |
| POST | `/api/categories` | 新增分類 | MANAGER ADMIN |
| PUT | `/api/categories/{id}` | 編輯分類 | MANAGER ADMIN |
| DELETE | `/api/categories/{id}` | 刪除分類 | MANAGER ADMIN |

### 酒單篩選參數（GET /api/menu）

```
GET /api/menu
  ?type=DRINK                     只看酒品
  &categoryId=1                   特定分類
  &available=true                 只看有貨的
```

---

## 酒譜

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/menu/{productId}/recipe` | 取得酒譜 | MANAGER ADMIN |
| GET | `/api/menu/{productId}/recipe-detail` | 取得酒譜含食材明細 | MANAGER ADMIN |
| POST | `/api/menu/{productId}/recipe` | 新增 / 更新酒譜 | MANAGER ADMIN |

> 飲品屬性維度（`drink_attribute_type` / `option` 與 `/api/attributes/*`）已於 `6e9103f` 整組移除。
> 菜單與酒譜的寫入權限已於 `458cac3` 由 BARTENDER 收斂至 MANAGER / ADMIN。

---

## 食材庫存

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/ingredients` | 取得所有食材 | MANAGER ADMIN |
| POST | `/api/ingredients` | 新增食材 | MANAGER ADMIN |
| PUT | `/api/ingredients/{id}` | 編輯食材 | MANAGER ADMIN |
| PATCH | `/api/ingredients/{id}/availability` | 標記缺貨 / 補貨（連動下架品項） | MANAGER ADMIN |
| DELETE | `/api/ingredients/{id}` | 刪除食材 | MANAGER ADMIN |

---

## 訂位

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| POST | `/api/reservations` | 新增訂位（交易內鎖桌驗證容量，防超訂） | Public |
| GET | `/api/reservations/slots?date=&partySize=&seatType=` | 查詢時段可訂性（依人數 + 座位區計算） | Public |
| GET | `/api/reservations/config` | 訂位頁設定（座位區人數上限、可提前天數） | Public |
| GET | `/api/reservations/my?phone=&code=` | 顧客查詢自己的訂位（手機 + 訂位代碼） | Public |
| POST | `/api/reservations/cancel` | 顧客自助取消（body 帶手機 + 訂位代碼） | Public |
| GET | `/api/reservations?date=` | 查詢訂位清單（篩選日期） | WAITER+ |
| PATCH | `/api/reservations/{id}/status` | 更新狀態（SEATED / CANCELLED / NO_SHOW / COMPLETED） | WAITER+ |

**防超訂規則**：一般座位（REGULAR）不併桌，每組需一張 `capacity ≥ partySize` 的桌子，
以 best-fit decreasing 裝箱檢查該時段（依各訂位自身 `duration_minutes` 判斷區間重疊）是否仍有可行桌位組合；
吧台（BAR_COUNTER）為座位池，時段內人數加總不得超過吧台總座位數，且單組上限 `bar_counter_max_party`（預設 3）。
鎖定（`is_locked`）或停用的桌位不列入可訂容量。建立訂位時整段包在交易內，
先 `SELECT ... FOR UPDATE` 鎖住可訂桌位再檢查 + 寫入，杜絕並發超訂。

---

## 結帳 / 發票

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/sessions/{sessionId}/payment/preview` | 預覽帳單（小計、服務費、合計、品項明細） | WAITER+ |
| POST | `/api/sessions/{sessionId}/payment` | 完成結帳（付款方式、均攤人數、發票載具） | WAITER+ |

> 電子發票隨結帳一併處理（`CheckoutRequest.carrierType` 有值才開票），尚無獨立的發票查詢端點。
> `EcpayInvoiceService` 仍是 stub：**未設定** `ecpay.*` 時回傳模擬號碼 `AB` + 8 碼付款 id；
> 一旦設定了金鑰卻沒實作 API 呼叫，`issue()` 會丟 `UnsupportedOperationException` 而讓結帳失敗——
> 串接完成前請維持金鑰為空。

---

## 報表（僅 ADMIN）

| Method | 路徑 | 說明 |
|---|---|---|
| GET | `/api/reports/daily?date=` | 指定日期營收、桌數、人數與逐小時分佈 |
| GET | `/api/reports/ranking?from=&to=&limit=` | 品項銷售排行（預設近 30 天，limit 上限 50） |
| GET | `/api/reports/monthly?year=&month=` | 當月每日收入 vs 去年同期 |

---

## 員工帳號（僅 ADMIN）

| Method | 路徑 | 說明 |
|---|---|---|
| GET | `/api/admin/users` | 取得員工清單 |
| POST | `/api/admin/users` | 新增員工帳號 |
| PUT | `/api/admin/users/{id}` | 編輯帳號（角色、聯絡資訊、強制改密） |
| DELETE | `/api/admin/users/{id}` | 停用帳號 |

---

## 系統設定（僅 ADMIN）

| Method | 路徑 | 說明 |
|---|---|---|
| GET | `/api/settings` | 取得所有設定值 |
| GET | `/api/settings/{key}` | 取得單一設定值 |
| PUT | `/api/settings` | 批次更新設定值 |
| PUT | `/api/settings/{key}` | 更新單一設定值 |

---

## 資料庫備份（僅 ADMIN）

| Method | 路徑 | 說明 |
|---|---|---|
| POST | `/api/backups` | 立即執行一次 mysqldump，回傳本次紀錄 |
| GET | `/api/backups` | 最近 20 筆備份紀錄 |

> 排程每日 03:00 自動執行，紀錄寫入 `backup_log`。

---

## 健康檢查

| 路徑 | 說明 | 角色 |
|---|---|---|
| `/actuator/health`、`/actuator/info` | 存活與版本資訊 | Public |
| `/actuator/**` 其餘 | 其他監控端點 | 已登入 |

---

## WebSocket 端點

```
連線端點：/ws（SockJS + STOMP），應用前綴 /app

訂閱頻道（需 JWT）：
  /topic/kitchen                   廚房新訂單推播
  /topic/bar                       吧台新訂單推播
  /topic/staff/pickup              服務生取餐通知
  /topic/tables                    桌位狀態更新

訂閱頻道（以 QR session token 驗證）：
  /topic/table/{token}/cart        客人端購物車同步
  /topic/table/{token}/orders      客人端訂單狀態同步
```

> 頻道授權在 `WebSocketConfig` 的 ChannelInterceptor 完成：`/topic/staff/**`、`/topic/kitchen`、
> `/topic/bar`、`/topic/tables` 必須帶有效 JWT；`/topic/table/{token}/*` 則比對該 token 是否為有效的 OPEN session。
