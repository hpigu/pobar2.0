# Pobar 酒吧系統 — 系統架構

## 整體架構圖

```
┌─────────────────────────────────────────────────────────┐
│                     Client 端                            │
│                                                         │
│  ┌─────────────────┐      ┌──────────────────────────┐  │
│  │   客人端瀏覽器    │      │   員工端瀏覽器 / 平板      │  │
│  │  (手機掃 QR)     │      │  (服務生 / 廚房 / 吧台)   │  │
│  └────────┬────────┘      └────────────┬─────────────┘  │
└───────────┼─────────────────────────────┼───────────────┘
            │ HTTPS                       │ HTTPS + WSS
            ▼                             ▼
┌─────────────────────────────────────────────────────────┐
│                  Cloudflare Tunnel                       │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                   酒吧本機電腦                            │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Vue3 Frontend (Nginx)                │   │
│  │                                                   │   │
│  │  /              客人端（酒單、點餐、訂位）          │   │
│  │  /staff/*       員工端（桌位、廚房、吧台、結帳）    │   │
│  │  /admin/*       後台（報表、庫存、設定）            │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │ HTTP / WebSocket               │
│  ┌──────────────────────▼───────────────────────────┐   │
│  │           Spring Boot 3.0 Backend                 │   │
│  │                                                   │   │
│  │  ┌─────────────┐  ┌──────────────┐               │   │
│  │  │ REST API    │  │ WebSocket    │               │   │
│  │  │ (/api/**)   │  │ (STOMP)      │               │   │
│  │  └──────┬──────┘  └──────┬───────┘               │   │
│  │         └────────────────┘                        │   │
│  │                  │                                │   │
│  │  ┌───────────────▼──────────────────────────┐    │   │
│  │  │           Service Layer                   │    │   │
│  │  │  Auth / Order / Menu / Table /            │    │   │
│  │  │  Reservation / Report / Invoice           │    │   │
│  │  └───────────────┬──────────────────────────┘    │   │
│  │                  │                                │   │
│  │  ┌───────────────▼──────────────┐                │   │
│  │  │     Spring Data JPA          │                │   │
│  │  └───────────────┬──────────────┘                │   │
│  └──────────────────┼───────────────────────────────┘   │
│                     │                                    │
│  ┌──────────────────▼───────────────────────────────┐   │
│  │                MySQL 8.0                          │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  排程工作：                                              │
│  ┌──────────────────────────────────────────────────┐   │
│  │  @Scheduled 每日備份 → mysqldump → Google Drive   │   │
│  │  @Scheduled 每分鐘掃描逾時訂位 → 自動取消          │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
            │                    │
            ▼                    ▼
    ┌───────────────┐   ┌─────────────────┐
    │  綠界 ECPay   │   │  Google Drive   │
    │  電子發票 API  │   │  備份儲存        │
    └───────────────┘   └─────────────────┘
```

---

## WebSocket 訊息規劃

所有 WebSocket 訊息走 STOMP，頻道依功能區分：

| 頻道 | 訂閱者 | 觸發時機 | 訊息內容 |
|---|---|---|---|
| `/topic/table/{sessionId}/orders` | 同桌所有客人手機 | 有人新增品項到購物車 | 購物車目前品項清單 |
| `/topic/kitchen` | KITCHEN 角色 | 新食物訂單成立 | 訂單明細 |
| `/topic/bar` | BARTENDER 角色 | 新酒品訂單成立 | 訂單明細 + 酒譜 |
| `/topic/staff/notify` | WAITER 角色 | 品項狀態變為 READY | 「A3 桌 威士忌酸酒 可取餐」 |
| `/topic/tables` | WAITER / MANAGER | 桌位狀態異動 | 更新後的桌位狀態清單 |
| `/topic/menu` | 所有客人端 | 品項上下架、售完 | 異動的品項 id 與狀態 |

---

## 後端模組拆分

```
com.pobar
│
├── controller
│   ├── AuthController          登入、登出、refresh token
│   ├── TableController         桌位 CRUD、開關桌、併桌
│   ├── SessionController       Table Session 管理、QR code 產生
│   ├── OrderController         下單、取消、修改、狀態更新
│   ├── MenuController          品項 CRUD、照片上傳
│   ├── CategoryController      分類 CRUD
│   ├── AttributeController     維度類型 + 選項 CRUD
│   ├── IngredientController    食材 CRUD、缺貨標記
│   ├── RecipeController        酒譜 CRUD
│   ├── ReservationController   訂位 CRUD、自動取消
│   ├── PaymentController       結帳、分帳計算
│   ├── InvoiceController       綠界電子發票串接
│   ├── ReportController        報表資料 API
│   ├── UserController          員工帳號管理
│   └── SettingController       系統設定
│
├── websocket
│   └── OrderWebSocketHandler   STOMP 訊息處理與廣播
│
├── service / service/impl      業務邏輯層
├── repository                  Spring Data JPA
├── entity                      20 張資料表對應
├── dto                         API 請求 / 回應物件
├── security                    JWT Filter、Role 權限設定
├── scheduler                   自動備份、逾時訂位取消
└── util                        QR code 產生、圖片上傳工具
```

---

## 前端頁面清單（Vue3）

### 客人端（不需登入）

| 路由 | 頁面 | 說明 |
|---|---|---|
| `/` | 酒單首頁 | 分類瀏覽 + 多選篩選（基酒、甜酸、濃淡、香氣） |
| `/menu/:id` | 品項詳細頁 | 完整屬性、照片、相似推薦 |
| `/table` | 點餐頁 | QR token 驗證、購物車、送出訂單 |
| `/table/orders` | 本桌訂單紀錄 | 查看已點品項與狀態 |
| `/reservation` | 訂位頁 | 填表、選座位類型與人數 |
| `/reservation/cancel` | 取消訂位頁 | 透過 cancel_token 取消 |

### 員工端（需登入）

| 路由 | 頁面 | 角色 |
|---|---|---|
| `/staff/login` | 登入 | 全部 |
| `/staff/tables` | 桌位管理（視覺化座位圖） | WAITER MANAGER ADMIN |
| `/staff/kitchen` | 廚房訂單顯示 | KITCHEN |
| `/staff/bar` | 吧台訂單顯示 + 酒譜 | BARTENDER |
| `/staff/checkout/:sessionId` | 結帳頁 | WAITER MANAGER ADMIN |
| `/staff/reservations` | 當日訂位清單 | WAITER MANAGER ADMIN |

### 後台（需登入，MANAGER / ADMIN）

| 路由 | 頁面 | 角色 |
|---|---|---|
| `/admin/reports` | 報表（折線/長條/圓餅圖） | MANAGER ADMIN |
| `/admin/inventory` | 食材庫存管理 | MANAGER ADMIN |
| `/admin/menu` | 菜單/酒單管理 | MANAGER ADMIN BARTENDER |
| `/admin/menu/new` | 新增品項 | MANAGER ADMIN BARTENDER |
| `/admin/menu/:id/edit` | 編輯品項 | MANAGER ADMIN BARTENDER |
| `/admin/reservations` | 完整訂位管理 | MANAGER ADMIN |
| `/admin/tables-setup` | 桌位配置（拖曳） | ADMIN |
| `/admin/attributes` | 酒單維度管理 | ADMIN |
| `/admin/users` | 員工帳號管理 | ADMIN |
| `/admin/settings` | 系統設定 | ADMIN |
