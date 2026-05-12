# Pobar 系統安全檢查報告

> 首次掃描：2026-05-12
> 最後更新：2026-05-12（修補後）
> 範圍：後端 Java code + Mapper SQL + 前端 Vue + nginx + Docker 設定

---

## 修補進度總覽

| # | 項目 | 嚴重度 | 狀態 | 備註 |
|---:|---|---|---|---|
| 1 | 訂位查詢被列舉 | 🔴 立刻修 | ✅ 已修 | 改成 phone+code 雙因子 |
| 2 | WebSocket 沒驗 JWT | 🟡 上 prod 前 | ✅ 已修 | SUBSCRIBE 階段攔截 |
| 3 | WebSocket origin 全開 | 🟡 上 prod 前 | ⏸ 待做 | 部署時改白名單 |
| 4 | nginx 沒 HTTPS | 🟡 上 prod 前 | ⏸ 待做（文件已備） | 部署選 Cloudflare / Let's Encrypt / ALB |
| 5 | CORS `*` + credentials | 🟡 上 prod 前 | ✅ 已修 | 改白名單機制，空字串=同 origin |
| 6 | XSS 漏 4 個 service | 🟡 上 prod 前 | ✅ 已修 | Reservation/User/Ingredient/Table 都 sanitize |
| 7 | Session token 在 URL | 🟡 上 prod 前 | ✅ 已修 | 改 `X-Session-Token` header |
| 8 | mysqldump `-p` 旗標 | 🟡 上 prod 前 | ⏸ 待做 | 改 `--defaults-extra-file`，使用者跳過 |
| 9 | JWT 在 localStorage | 🟡 上 prod 前 | ⏸ 待做 | 上 HTTPS 後考慮改 HttpOnly cookie |
| 10 | 無 Refresh Token | 🟢 加分項 | ✅ 已修 | Access 15分 + Refresh 7/30天，含「本機記住」 |
| 11 | ADMIN 沒 2FA | 🟢 加分項 | ⏸ 待做 | 未來再加 TOTP |
| 12 | 沒 WAF | 🟢 加分項 | ⏸ 待做 | 上 Cloudflare 後自帶免費 WAF |

**完成 7/12（含全部 🔴 + 大半 🟡）**

剩下 5 項都是**部署 / 運維層**或**進階加固**，application code 層級基本完工。

---

## 已修補項目詳細

### ✅ #1 訂位查詢列舉防護

**問題**：`GET /api/reservations/my?phone=xxxx` 任何人帶任意手機就能列出該手機所有訂位。

**修補**：
- DB schema 加 `reservation.booking_code` VARCHAR(10) + `idx_phone_code` 索引（migration 003）
- 建立預約時自動產生 8 位易讀代碼（去除 0/O/1/I/L，用 `SecureRandom`）
- API 改成 `GET /api/reservations/my?phone=xxx&code=ABCD2345`，缺一不可
- 前端：訂位成功頁顯示代碼，查詢頁需填 phone + code

**效果**：攻擊者就算掃中電話，沒對應的 8 位代碼也查不到。code space = 31^8 ≈ 8.5 × 10¹¹，配合 RateLimit 60req/min/IP 不可能爆破。

---

### ✅ #2 WebSocket JWT 驗證

**問題**：任何陌生人都能連 `/ws` 訂閱 `/topic/staff/pickup` 等取得即時推播。

**修補**：
- `WebSocketConfig` 加 `ChannelInterceptor`
- **CONNECT 階段**：有帶 `Authorization: Bearer xxx` 就建立認證；無 JWT 也放行（顧客 QR 頁面用）
- **SUBSCRIBE 階段**依 destination 分類：
  - `/topic/staff/**`、`/topic/kitchen`、`/topic/bar`、`/topic/tables` → 必須有有效 JWT
  - `/topic/table/{token}/*` → 用 session token 驗證
  - 其他 → 必須有 JWT
- 前端 `useWebSocket.js` 自動帶 `connectHeaders: { Authorization }`

**效果**：staff 即時推播不會洩漏給匿名連線；顧客訂閱自己桌位資訊仍可運作。

---

### ✅ #5 CORS 白名單

**問題**：`config.setAllowedOriginPatterns(List.of("*"))` + `setAllowCredentials(true)` 危險組合。

**修補**：
- `SecurityConfig` 讀 `cors.allowed-origins` properties（逗號分隔白名單）
- 空字串 → 不允許跨域（同 origin 模式，配合 nginx 反代 OK）
- 含 `*` → 開放全部但**強制關掉 credentials**（避免漏洞）
- 白名單 → 啟用 `setAllowCredentials(true)`
- `setExposedHeaders(List.of("Authorization"))` 讓前端可讀 token

**效果**：上 prod 配前後端分 domain 時設 `CORS_ALLOWED_ORIGINS=https://www.your-domain.com` 即可。

---

### ✅ #6 XSS 補強

| 位置 | 欄位 |
|---|---|
| `ReservationServiceImpl.create` | customerName、customerPhone、notes |
| `IngredientServiceImpl.fromRequest` | name、unit |
| `UserManagementServiceImpl.create/update` | account、email、phone |
| `TableServiceImpl.saveTable` | name |

所有 user-input 字串欄位進 DB 前都用 `XssUtil.sanitize()` 過濾（Jsoup `Safelist.none()` 完全清乾淨）。

**效果**：後台列表頁渲染這些欄位時不會被 `<script>` 注入。

---

### ✅ #7 Session token URL → Header

**問題**：`/api/cart/{token}` 把 token 寫進 nginx access log、瀏覽器歷史、Referer header。

**修補**：
- 後端：`CartController`、`OrderController` 改成 `@RequestHeader("X-Session-Token")`
- 前端：`CustomerOrderPage` 從 URL path param 讀 token 一次後存 `localStorage`，axios interceptor 自動帶 header
- `stores/cart.js` URL 改為 `/api/cart`、`/api/cart/items`

**效果**：除了顧客首次掃 QR 進站那一次（`/order/xxx`）會在 access log 留紀錄外，後續所有 cart/order API 都不會在 log 中出現 token。

---

### ✅ #10 Refresh Token + 本機記住

**設計**：雙 token 機制
- **Access Token**：JWT，15 分鐘，所有 API 用此
- **Refresh Token**：256-bit 隨機字串（base64url），DB hashed 儲存，只能呼叫 `/api/auth/refresh` 換新 access

**TTL**：
- 一般裝置：7 天
- 勾「本機記住」（trusted）：30 天

**安全特性**：
- **Token rotation**：每次 refresh 都 revoke 舊 refresh、發新 refresh，舊的被重放會被拒絕
- 改密碼後 `revokeAllByUserId(userId)` 撤銷該用戶所有 refresh token，強制所有裝置重登
- 登出 revoke 該 refresh + access 進黑名單
- `AuthCleanupScheduler` 每日清過期 refresh token 跟 revoked 7 天以上的紀錄

**前端**：
- LoginPage 加「本機記住（30 天免重登）」checkbox + 提示「僅在店內信任設備勾選」
- axios 攔截 401 自動 refresh + retry，多個並發請求共用同一次 refresh
- Refresh 失敗 → 清登入狀態跳 login

**對使用者影響**：升級後既有 access token 用完後沒 refresh token，會被迫重登一次。之後改善的體驗會比舊版好（即使勾沒記住也 7 天免重登）。

---

## 仍未修補項目

### ⏸ #3 WebSocket origin 限縮
**現況**：`setAllowedOriginPatterns("*")`
**待做**：部署時改成明確 domain 白名單
**為什麼還沒做**：與 #4 HTTPS 一樣，要拿到正式 domain 後才有對象可填

### ⏸ #4 HTTPS
**現況**：nginx 只開 80 port
**待做**：部署時擇一：
- Cloudflare（推薦，免費 SSL+WAF+CDN）
- nginx + Let's Encrypt（自架）
- AWS ALB + ACM（上 AWS 後）

詳細步驟見 [`docs/env-setup.md`](env-setup.md) 最後一段。

### ⏸ #8 mysqldump 密碼旗標
**現況**：`BackupScheduler` 用 `-p` + 密碼明文傳給 mysqldump（process list 可見）
**風險**：低（只有同 host 進來才看得到，且 docker container 內 ps 範圍小）
**待做**：改用 `--defaults-extra-file=/tmp/my.cnf` 寫入臨時 cnf 檔
**使用者選擇跳過**

### ⏸ #9 JWT 改 HttpOnly Cookie
**現況**：token 存 `localStorage`，遇到 XSS 會被竊
**風險中度**：但因為已套 XSS sanitize + Vue 預設 escape，實際攻擊面小
**待做**：上 HTTPS 後改 HttpOnly + Secure + SameSite=Strict cookie，搭配 CSRF token
**工作量**：2-3 天，可延後

### ⏸ #11 ADMIN 2FA
**待做**：給 ADMIN 帳號加 TOTP（Google Authenticator 之類）
**效益**：即使密碼被洩漏，沒手機 6 位碼也進不來
**工作量**：3-5 天（含前端 QR 設定流程）

### ⏸ #12 WAF
**待做**：上 Cloudflare（domain 接過去就送）或 AWS WAF
**效益**：應用層防禦之外的雲端邊緣防護（DDoS、bot、地理封鎖）
**工作量**：DNS 切換約 1 小時

---

## 已驗證**零風險**的項目

- **SQL Injection**：所有 SQL 都 parameterized，沒 `${}` 直接注入
- **CSRF**：JWT-based、Authorization header 傳遞，跨站攻擊無效
- **Vertical Authorization**：所有後台 endpoint 都有 `@PreAuthorize`
- **檔案上傳**：StorageService 統一驗副檔名、大小、UUID 化檔名
- **500 錯誤訊息**：`server.error.include-stacktrace=never`，不洩 stacktrace
- **Brute force**：雙維度鎖（account+ip / 純 IP）+ RateLimitFilter

---

## 整體安全姿態

**等級評估**：⭐⭐⭐⭐☆（4/5）— **可上 SIT 測試環境，但 prod 還需補 HTTPS**

主要強項：
- 多層防護（rate limit + lockout + audit log）
- JWT 雙 token + token rotation
- 細緻權限分層（@PreAuthorize + JwtAuthFilter mcp 旗標）
- 統一錯誤碼 + DTO 防 entity 外洩

主要弱項（皆為部署層）：
- 無 HTTPS（純 HTTP 高風險）
- 無 WAF / DDoS 防護
- 無 2FA

**建議部署順序**：
1. 申請 domain + Cloudflare（同時拿到 #3、#4、#12）
2. 設定 `CORS_ALLOWED_ORIGINS` 明確 domain
3. 上線後依需要加 2FA、HttpOnly cookie、mysqldump 強化
