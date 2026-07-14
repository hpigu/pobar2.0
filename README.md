<div align="center">

# 🍹 Pobar 2.0

### 智慧吧台點餐管理系統

全端酒吧管理平台，整合客戶掃碼自助點餐、即時廚房 / 吧台顯示、桌況管理、線上訂位與後台報表，一站式涵蓋完整營運流程。

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-42B883?logo=vue.js&logoColor=white)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docker.com/)

</div>

---

## 目錄

- [系統概覽](#系統概覽)
- [角色功能](#角色功能)
  - [👤 客人](#-客人public)
  - [🛎️ 服務生](#️-服務生waiter)
  - [🍸 調酒師](#-調酒師bartender)
  - [👨‍🍳 廚師](#-廚師kitchen)
  - [🔧 管理員](#-管理員admin--manager)
- [部署說明](#部署說明)
- [本地開發](#本地開發)
- [專案結構](#專案結構)

---

## 系統概覽

| 層級 | 技術 |
|------|------|
| **後端** | Java 17 · Spring Boot 3.0 · Spring Security · MyBatis Plus |
| **前端** | Vue 3 · Vite · Pinia · Element Plus · ECharts |
| **資料庫** | MySQL 8.0 |
| **即時通訊** | WebSocket / STOMP · SockJS |
| **認證** | JWT（雙 Token：Access + Refresh） |
| **容器化** | Docker · Docker Compose · Nginx |

**角色對應頁面**

| 角色 | 頁面路徑 | 說明 |
|------|----------|------|
| 客人（無帳號） | `/order/:token`、`/reservation` | QR Code 掃碼進入 |
| WAITER | `/staff` | 桌況、結帳、訂位 |
| BARTENDER | `/bar` | 吧台飲品顯示 |
| KITCHEN | `/kitchen` | 廚房食物顯示 |
| MANAGER | `/staff`、`/admin`（除使用者管理） | 含服務生所有操作 |
| ADMIN | 全部頁面 | 完整後台權限 |

---

## 角色功能

---

### 👤 客人（Public）

> 無需帳號，掃描桌邊 QR Code 即可開始使用

#### 📋 自助點餐 `/order/:token`

- 依分類（食物 / 飲品）瀏覽菜單，支援關鍵字搜尋
- 查看飲品配方與食材說明
- 加入購物車並附上備註（例：「食材過敏」）
- 調整數量後一次送出，多支手機同桌即時同步
- 追蹤每筆品項狀態：

  ```
  待處理 (PENDING)  ──▶  製作中 (IN_PROGRESS)  ──▶  完成 (READY)
  ```

- 結帳前可預覽帳單明細

#### 📅 線上訂位 `/reservation`

- 瀏覽未來 10 天（可設定）內的可用時段，時段為 30 分鐘一格
- 選擇座位區（一般座位 / 吧台）與人數，時段可訂性依「人數 + 座位區」即時計算
  - 一般座位：不併桌，單組人數上限 = 最大單桌容量，以裝箱演算法檢查該時段是否還湊得出桌子
  - 吧台：以座位池計算，單組最多 3 位（可設定），4 位以上請訂一般座位
- 後端建立訂位時於交易內鎖定桌位重新驗證容量，並發下也不會超訂
- 填寫姓名、電話完成預訂，系統回傳 8 碼訂位碼
- 輸入電話 + 訂位碼可查詢訂單狀態或線上取消

---

### 🛎️ 服務生（WAITER）

> 負責桌況管理、點餐協助、訂位接待與結帳 `/staff`

#### 🪑 桌況管理

- 一覽全場桌位即時狀態（空桌 / 使用中 / 合併中）
- 開桌 / 關桌，自動產生 QR Code 供客人掃碼點餐
- 合併多桌（適用大型聚會），支援 N 張桌共用同一帳單
- 設定入座人數

#### 📦 訂單管理

- 查看指定桌位所有品項與目前狀態
- 手動更新品項狀態（代廚房 / 吧台更新）
- 取消品項（自動記錄操作人與時間）
- 修改備註與數量（僅限待處理品項）

#### 📅 訂位管理

- 查看當日所有訂位列表
- 為訂位客人安排座位、更新狀態：

  ```
  已確認 (CONFIRMED)  ──▶  已入座 (SEATED)  ──▶  已完成 / 未到 (COMPLETED / NO_SHOW)
  ```

- 逾時 10 分鐘自動標記 NO_SHOW（可後台調整時限）

#### 💳 結帳

- 預覽帳單（小計、服務費率、總計）
- 支援 **現金 / 刷卡 / 其他** 付款方式
- 分帳計算（多人均攤，自動計算每人金額）
- 開立統一發票（手機載具 / 自然人憑證 / 紙本二聯式）

---

### 🍸 調酒師（BARTENDER）

> 負責吧台飲品製作與食材庫存管理 `/bar`

#### 📺 吧台顯示屏

- 僅顯示**飲品類**訂單，排除食物干擾
- 新訂單透過 WebSocket 即時推送 + 音效提示
- 點擊更新製作狀態（待處理 → 製作中 → 完成）
- WebSocket 斷線時自動降級為輪詢模式（每 30 秒）

#### 🧴 食材管理 `/admin/ingredients`

- 新增 / 編輯各類食材（基酒、利口酒、糖漿、果汁、裝飾等）
- 標記食材**缺貨**，系統自動下架所有使用該食材的飲品
- 食材上架後自動恢復飲品可販售狀態

**食材分類**

| 分類 | 內容 |
|------|------|
| BASE_SPIRIT | 基酒（威士忌、琴酒、伏特加…） |
| LIQUEUR | 利口酒 |
| WINE / BEER | 葡萄酒 / 啤酒 |
| SYRUP | 糖漿 |
| JUICE | 果汁 |
| FRESH | 新鮮食材 |
| GARNISH | 裝飾（薄荷葉、檸檬片…） |

---

### 👨‍🍳 廚師（KITCHEN）

> 負責廚房食物品項的製作流程 `/kitchen`

#### 📺 廚房顯示屏（KDS）

- 僅顯示**食物類**訂單，與吧台畫面完全分離
- 新訂單透過 WebSocket 即時推送 + 音效提示
- 點擊更新製作狀態（待處理 → 製作中 → 完成）
- WebSocket 斷線時自動降級為輪詢模式（每 30 秒）

---

### 🔧 管理員（ADMIN / MANAGER）

> ADMIN 具備完整後台權限；MANAGER 等同 ADMIN，但無法管理員工帳號

#### 🍽️ 菜單管理 `/admin/menu`

- 新增 / 編輯 / 刪除商品分類（食物 / 飲品），可設定顯示排序
- 管理品項：中英文名稱、售價、圖片上傳
- 設定限時供應時段（開始 / 結束時間）
- 一鍵下架 / 上架，臨時缺貨不影響品項資料
- 建立雞尾酒配方：綁定食材、設定用量與製作說明

#### 🪑 桌位管理 `/admin/tables`

- 新增 / 編輯 / 刪除桌位，設定桌型（一般桌 / 吧台座）與容納人數
- 可視化平面圖坐標定位（x, y 軸）
- 鎖定 / 解鎖桌位（停止接受新訂位）

#### 📊 報表分析 `/admin/reports`（僅 ADMIN）

- 每日逐小時營收走勢圖
- 近 30 天熱銷品項排行（前 20 名）
- 本月 vs 上月營收比較
- ECharts 互動式視覺化圖表

#### 👥 員工管理 `/admin/users`（僅 ADMIN）

- 建立員工帳號並指派角色
- 編輯聯絡資訊（電子郵件、電話）
- 強制下次登入修改密碼
- 停用 / 啟用帳號

#### ⚙️ 系統設定 `/admin/settings`（僅 ADMIN）

| 設定項目 | 預設值 | 說明 |
|----------|--------|------|
| 服務費率 | 10% | 結帳時自動計算加入帳單 |
| 訂位時長 | 120 分鐘 | 每筆訂位的佔桌時間 |
| 訂位可提前天數 | 10 天 | 線上訂位最多可提前預約的天數 |
| 吧台單組人數上限 | 3 位 | 吧台訂位單組上限，超過須訂一般座位 |
| No-show 自動取消 | 10 分鐘 | 逾時自動標記未到 |
| 商業日重置時間 | 凌晨 4 時 | 適用通宵營業場所 |
| 食物 / 飲品供應時段 | 可設定 | 分別控制廚房與吧台服務時間 |
| 年齡確認提示 | 可開關 | 客人首次進入點餐頁的提醒彈窗 |
| 點餐速率限制 | 5 次 / 分鐘 | 防止意外重複大量送單 |
| 最大品項數 / 批次 | 20 項 | 單次訂單可包含的最大品項數 |
| 前端網址 | localhost | QR Code 內嵌的對外網址 |

---

## 部署說明

### 環境需求

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)（開機自動啟動即可）

### 首次設定（工程師操作，僅需一次）

```bash
# 1. 複製環境變數範本並填入密碼
cp .env.example .env
# 編輯 .env：填入 MYSQL_ROOT_PASSWORD、DB_PASSWORD、JWT_SECRET、INIT_ADMIN_PASSWORD

# 2. 啟動系統（首次會自動 build，約 5–10 分鐘）
docker compose up -d --build

# 3. 確認啟動後，清空 .env 中的 INIT_ADMIN_PASSWORD 並重啟後端
docker compose restart backend
```

### 每日操作（員工）

`scripts/` 資料夾內有桌面捷徑，複製到桌面即可使用：

| 捷徑 | 說明 |
|------|------|
| `啟動系統.bat` | 雙擊啟動，自動開啟瀏覽器 |
| `關閉系統.bat` | 雙擊關閉所有服務 |
| `檢查狀態.bat` | 確認系統是否正常運作 |

> 電腦開機後等待 Docker Desktop 啟動完成（約 30 秒），再雙擊「啟動系統」。

---

## 本地開發

### 環境需求

| 工具 | 版本 |
|------|------|
| Java | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 18+ |

### 快速啟動

```bash
# 1. 建立資料庫
mysql -u root -p -e "CREATE DATABASE pobar CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p pobar < sql/schema.sql

# 2. 複製後端設定
cp src/main/resources/application.properties src/main/resources/application-local.properties
# 編輯 application-local.properties，填入資料庫密碼、JWT 密鑰、初始管理員密碼

# 3. 啟動後端（http://localhost:8080）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. 啟動前端（http://localhost:5173）
cd frontend && npm install && npm run dev
```

### 注意事項

- **業務日**從每天 04:00 開始計算，支援跨夜營業
- **購物車**為 in-memory 儲存，重啟後清空；多支手機透過 WebSocket 同步
- **每日備份**：03:00 自動執行 mysqldump，輸出至 `./backups/`
- **ECPay 電子發票**：目前為 stub，正式上線前需在 `application.properties` 填入商家金鑰並實作 `EcpayInvoiceServiceImpl.issue()`

---

## 專案結構

```
pobar2.0/
├── src/main/java/com/pobar/
│   ├── controller/       # REST 端點
│   ├── service/impl/     # 業務邏輯
│   ├── mapper/           # MyBatis Plus mapper
│   ├── entity/           # DB 實體
│   ├── dto/              # 請求 / 回應 DTO
│   ├── security/         # JWT、Security Config、Rate Limit
│   ├── logging/          # Audit AOP
│   ├── scheduler/        # 定時任務（訂位 No-show、備份）
│   └── util/             # XSS 工具
├── sql/
│   └── schema.sql        # 資料庫 DDL
├── frontend/
│   ├── src/views/        # 頁面元件
│   ├── src/stores/       # Pinia 狀態（auth、cart）
│   ├── src/api/          # Axios instance
│   ├── src/router/       # Vue Router
│   └── src/composables/  # WebSocket composable
├── scripts/              # 員工用桌面捷徑
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

---

<div align="center">

Pobar 2.0 &nbsp;·&nbsp; Built for the hospitality industry

</div>
