# Pobar 酒吧系統 — API 規劃

## 認證

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| POST | `/api/auth/login` | 員工登入，回傳 JWT | Public |
| POST | `/api/auth/logout` | 登出（前端清除 token） | 已登入 |
| GET | `/api/auth/me` | 取得目前登入者資訊 | 已登入 |
| PUT | `/api/auth/password` | 修改密碼 | 已登入 |

---

## 桌位

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/tables` | 取得所有桌位（含狀態） | WAITER+ |
| POST | `/api/tables` | 新增桌位 | ADMIN |
| PUT | `/api/tables/{id}` | 編輯桌位（名稱、容量、座標、鎖定） | ADMIN |
| DELETE | `/api/tables/{id}` | 刪除桌位 | ADMIN |
| POST | `/api/tables/sessions` | 開桌（單桌或多桌併桌），回傳 session + QR token | WAITER+ |
| DELETE | `/api/tables/sessions/{sessionId}` | 關桌（結帳後） | WAITER+ |
| POST | `/api/tables/sessions/{sessionId}/merge` | 併桌（追加更多 table 到 session） | WAITER+ |
| GET | `/api/tables/sessions/{sessionId}` | 查詢 session 狀態（客人掃 QR 後呼叫） | Public |

---

## 點餐

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| POST | `/api/orders` | 送出訂單（一次送出多個品項） | Public（帶 token） |
| GET | `/api/orders/session/{sessionId}` | 查詢本桌所有訂單紀錄 | Public（帶 token） |
| PUT | `/api/orders/items/{itemId}/status` | 更新品項狀態（IN_PROGRESS / READY） | KITCHEN BARTENDER |
| DELETE | `/api/orders/items/{itemId}` | 取消品項 | WAITER+ |
| PUT | `/api/orders/items/{itemId}` | 修改品項備註、數量 | WAITER+ |

---

## 菜單 / 酒單

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/menu` | 取得所有上架品項（含屬性），支援篩選參數 | Public |
| GET | `/api/menu/{id}` | 取得單一品項詳細資訊 | Public |
| POST | `/api/menu` | 新增品項 | MANAGER ADMIN BARTENDER |
| PUT | `/api/menu/{id}` | 編輯品項 | MANAGER ADMIN BARTENDER |
| DELETE | `/api/menu/{id}` | 下架品項（軟刪除） | MANAGER ADMIN |
| PUT | `/api/menu/{id}/availability` | 切換臨時售完 | MANAGER ADMIN BARTENDER |
| POST | `/api/menu/{id}/image` | 上傳品項照片 | MANAGER ADMIN BARTENDER |
| GET | `/api/categories` | 取得分類清單 | Public |
| POST | `/api/categories` | 新增分類 | MANAGER ADMIN |
| PUT | `/api/categories/{id}` | 編輯分類 | MANAGER ADMIN |
| DELETE | `/api/categories/{id}` | 刪除分類 | MANAGER ADMIN |

---

## 酒單篩選參數（GET /api/menu）

```
GET /api/menu
  ?type=DRINK                     只看酒品
  &categoryId=1                   特定分類
  &attributeOptions=1,3,7         多選屬性選項 id（AND 條件）
  &available=true                 只看有貨的
```

---

## 酒譜

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/menu/{productId}/recipe` | 取得酒譜（調酒師點餐時用） | BARTENDER+ |
| POST | `/api/menu/{productId}/recipe` | 新增酒譜 | MANAGER ADMIN BARTENDER |
| PUT | `/api/menu/{productId}/recipe` | 更新酒譜 | MANAGER ADMIN BARTENDER |

---

## 酒單屬性維度

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/attributes` | 取得所有維度類型 + 選項 | Public |
| POST | `/api/attributes/types` | 新增維度類型 | ADMIN |
| PUT | `/api/attributes/types/{id}` | 編輯維度類型 | ADMIN |
| DELETE | `/api/attributes/types/{id}` | 刪除維度類型 | ADMIN |
| POST | `/api/attributes/types/{typeId}/options` | 新增選項 | ADMIN |
| PUT | `/api/attributes/options/{id}` | 編輯選項 | ADMIN |
| DELETE | `/api/attributes/options/{id}` | 刪除選項 | ADMIN |

---

## 食材庫存

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/ingredients` | 取得所有食材 | MANAGER ADMIN |
| POST | `/api/ingredients` | 新增食材 | MANAGER ADMIN |
| PUT | `/api/ingredients/{id}` | 編輯食材 | MANAGER ADMIN |
| DELETE | `/api/ingredients/{id}` | 刪除食材 | MANAGER ADMIN |
| PUT | `/api/ingredients/{id}/availability` | 標記缺貨 / 補貨（連動下架品項） | MANAGER ADMIN |

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
| GET | `/api/payments/session/{sessionId}/preview` | 預覽帳單（小計、服務費、總計） | WAITER+ |
| POST | `/api/payments` | 完成結帳 | WAITER+ |
| POST | `/api/invoices/{paymentId}` | 開立電子發票（呼叫綠界） | WAITER+ |
| GET | `/api/invoices/{paymentId}` | 查詢發票狀態 | WAITER+ |

---

## 報表

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/reports/daily?date=` | 指定日期營收總計 | MANAGER ADMIN |
| GET | `/api/reports/revenue?from=&to=` | 區間每日營收折線圖資料 | MANAGER ADMIN |
| GET | `/api/reports/sales-ranking?from=&to=` | 品項銷售排行 | MANAGER ADMIN |
| GET | `/api/reports/monthly?year=` | 全年每月比較 | MANAGER ADMIN |

---

## 員工帳號

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/users` | 取得員工清單 | ADMIN |
| POST | `/api/users` | 新增員工帳號 | ADMIN |
| PUT | `/api/users/{id}` | 編輯帳號（角色、啟用狀態） | ADMIN |
| POST | `/api/users/{id}/reset-password` | 重設密碼 | ADMIN |

---

## 系統設定

| Method | 路徑 | 說明 | 角色 |
|---|---|---|---|
| GET | `/api/settings` | 取得所有設定值 | ADMIN |
| PUT | `/api/settings` | 批次更新設定值 | ADMIN |

---

## WebSocket 端點

```
連線端點：ws://host/ws（STOMP）

訂閱頻道：
  /topic/table/{sessionId}/cart    客人端購物車同步
  /topic/kitchen                   廚房新訂單推播
  /topic/bar                       吧台新訂單推播
  /topic/staff/pickup              服務生取餐通知
  /topic/tables                    桌位狀態更新
  /topic/menu/availability         品項上下架即時更新

發送端點（客人端送出購物車）：
  /app/cart/{sessionId}/add        加入品項
  /app/cart/{sessionId}/remove     移除品項
  /app/order/{sessionId}/submit    送出訂單
```
