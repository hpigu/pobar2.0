# Pobar 酒吧系統 — 系統架構

> 本文對照實作整理。歷史版本描述的 Spring Data JPA、Google Drive 備份、`/staff/*` 巢狀路由、
> 酒單屬性維度等皆已不存在，內容已依現行程式碼更新。

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
│         Cloudflare Tunnel（對外方案，可選）                │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│              酒吧本機電腦（docker compose）                │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │        Vue3 Frontend (Nginx, port 80)             │   │
│  │                                                   │   │
│  │  /order/:token   客人點餐      /reservation  訂位   │   │
│  │  /staff  /kitchen  /bar        員工作業頁面         │   │
│  │  /admin/*                      後台（報表、設定）    │   │
│  │                                                   │   │
│  │  反向代理 /api、/ws、/uploads、/actuator → :8080     │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │ HTTP / WebSocket               │
│  ┌──────────────────────▼───────────────────────────┐   │
│  │           Spring Boot 3.0 Backend                 │   │
│  │                                                   │   │
│  │  ┌─────────────┐  ┌──────────────┐               │   │
│  │  │ REST API    │  │ WebSocket    │               │   │
│  │  │ (/api/**)   │  │ (STOMP/ws)   │               │   │
│  │  └──────┬──────┘  └──────┬───────┘               │   │
│  │         └────────┬───────┘                        │   │
│  │  ┌───────────────▼──────────────────────────┐    │   │
│  │  │  Spring Security（內含 JwtAuthFilter）      │    │   │
│  │  │  → RequestLoggingFilter → RateLimitFilter │    │   │
│  │  └───────────────┬──────────────────────────┘    │   │
│  │  ┌───────────────▼──────────────────────────┐    │   │
│  │  │           Service Layer                   │    │   │
│  │  │  Auth / Order / Cart / Menu / Table /     │    │   │
│  │  │  Reservation / Payment / Report / Setting │    │   │
│  │  │  （@Audit AOP 記錄後台操作）                │    │   │
│  │  └───────────────┬──────────────────────────┘    │   │
│  │  ┌───────────────▼──────────────┐                │   │
│  │  │   MyBatis Plus (mapper)      │                │   │
│  │  └───────────────┬──────────────┘                │   │
│  └──────────────────┼───────────────────────────────┘   │
│  ┌──────────────────▼───────────────────────────────┐   │
│  │                MySQL 8.0                          │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  排程工作（@Scheduled）：                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  每日 03:00  mysqldump → /app/backups volume      │   │
│  │  每日 04:00  清過期 JWT 黑名單 / IP 鎖 / refresh   │   │
│  │  每 60 秒    掃描逾時訂位 → 自動取消                │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  Volumes：mysql-data / uploads / backups / logs          │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
                      ┌───────────────┐
                      │  綠界 ECPay   │
                      │ 電子發票（stub）│
                      └───────────────┘
```

備份目前只落在本機的 `backups` volume，沒有異地備援；要送上雲端需另接。
圖片儲存可切換 `STORAGE_TYPE=local`（`uploads` volume）或 `s3`。

**Filter 順序**：`springSecurityFilterChain` 的註冊順序為 -100，`RequestLoggingFilter` 標了 `@Order(1)`，
而 `RateLimitFilter` 只是 `@Component`（預設最低優先），因此限流實際發生在認證／授權**之後**。
對登入端點（`permitAll`）的暴力破解防護不受影響，但已被 401 擋掉的請求不會提早被限流攔下。

---

## WebSocket 訊息

所有訊息走 SockJS + STOMP，連線端點 `/ws`，應用前綴 `/app`。
授權在 `WebSocketConfig` 的 ChannelInterceptor 完成。

| 頻道 | 訂閱者 | 觸發時機 | 訊息內容 |
|---|---|---|---|
| `/topic/table/{token}/cart` | 同桌所有客人手機 | 購物車增減品項 | 購物車目前品項清單 |
| `/topic/table/{token}/orders` | 同桌所有客人手機 | 送出訂單後 | 本桌訂單品項清單 |
| `/topic/kitchen` | KITCHEN（需 JWT） | 新食物訂單成立 | 出餐品項 |
| `/topic/bar` | BARTENDER（需 JWT） | 新酒品訂單成立 | 出餐品項 |
| `/topic/staff/pickup` | WAITER（需 JWT） | 品項狀態變為 READY | 可取餐的品項 |
| `/topic/tables` | WAITER / MANAGER（需 JWT） | 桌位狀態異動 | 更新後的桌位清單 |

`/topic/staff/**`、`/topic/kitchen`、`/topic/bar`、`/topic/tables` 必須帶有效 JWT；
`/topic/table/{token}/*` 以該 token 是否為有效 OPEN session 驗證。

---

## 後端模組拆分

```
com.pobar
│
├── controller
│   ├── AuthController              登入、refresh、登出、改密、me
│   ├── TableController             桌位 CRUD、開關桌、併桌、QR session 查詢
│   ├── CartController              客人端購物車（in-memory）
│   ├── OrderController             下單、出餐看板、狀態更新、取消/修改品項
│   ├── MenuController              分類、品項 CRUD、圖片上傳、酒譜
│   ├── IngredientController        食材 CRUD、缺貨標記（連動下架）
│   ├── ReservationController       訂位、時段可訂性、顧客自助查詢/取消、員工管理
│   ├── PaymentController           帳單預覽、結帳（含均攤與發票載具）
│   ├── ReportController            日報、銷售排行、當月 vs 去年
│   ├── UserManagementController    員工帳號管理
│   ├── SettingController           系統設定
│   └── BackupController            手動觸發備份、備份紀錄查詢
│
├── service / service/impl          業務邏輯層
├── mapper                          MyBatis Plus mapper（+ resources/mapper/*.xml）
├── entity                          資料表對應（20 個 entity 類別）
├── dto                             API 請求 / 回應物件（依模組分子套件）
├── common                          Result 統一回應、ErrorCode 錯誤碼表
├── exception                       BusinessException、GlobalExceptionHandler
├── config                          MyBatisPlus、WebSocket、Web（CORS / 靜態資源）、MetaObjectHandler
├── security                        JwtUtil、JwtAuthFilter、RateLimitFilter、SecurityConfig、AuthUser
├── storage                         StorageService（Local / S3 可切換）
├── logging                         @Audit AOP、RequestLoggingFilter
├── scheduler                       每日備份、逾時訂位取消、Token 清理
├── runner                          InitAdminRunner（首次啟動建立 ADMIN）
└── util                            XssUtil
```

WebSocket 沒有獨立的 handler 類別：訊息由各 service 透過 `SimpMessagingTemplate` 廣播，
連線與訂閱授權集中在 `config/WebSocketConfig`。

---

## 前端頁面清單（Vue3）

路由定義於 `frontend/src/router/index.js`，角色檢查在 `router.beforeEach`。

### 客人端（不需登入）

| 路由 | 頁面 | 說明 |
|---|---|---|
| `/order/:token` | 點餐頁 | QR token 驗證、分類瀏覽、搜尋、購物車、送出訂單、中英切換 |
| `/reservation` | 訂位頁 | 選日期／座位區／人數／時段、查詢與自助取消（同一頁 dialog） |

### 員工端（需登入）

| 路由 | 頁面 | 角色 |
|---|---|---|
| `/login` | 登入 | 全部 |
| `/change-password` | 強制／自主改密 | 已登入 |
| `/staff` | 服務生工作台（桌位圖、訂單、訂位、結帳） | WAITER MANAGER ADMIN |
| `/kitchen` | 廚房出餐看板 | KITCHEN MANAGER ADMIN |
| `/bar` | 吧台出餐看板 | BARTENDER MANAGER ADMIN |

### 後台

| 路由 | 頁面 | 角色 |
|---|---|---|
| `/admin/reports` | 營收報表（ECharts） | ADMIN |
| `/admin/menu` | 品項／分類／酒譜管理 | ADMIN MANAGER |
| `/admin/tables` | 桌位配置 | ADMIN MANAGER |
| `/admin/ingredients` | 食材庫存管理 | ADMIN MANAGER |
| `/admin/settings` | 系統設定 | ADMIN |
| `/admin/users` | 員工帳號管理 | ADMIN |

> `/admin` 進入時依角色轉址：ADMIN → `/admin/reports`，MANAGER → `/admin/menu`。

---

## 測試

`frontend/e2e/` 為 Playwright E2E，直接打 docker compose 起的環境，
6 支 spec／13 個案例涵蓋顧客點餐、訂位、服務生、出餐看板、後台與登入權限，
全程錄影（`test-results/`）並產生 trace。詳見 [README 的自動化測試章節](../README.md#自動化測試)。
