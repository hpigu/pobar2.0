# Pobar 2.0 — 酒吧管理系統

Spring Boot 3.0 後端 + Vue 3 前端的全端酒吧管理系統，功能涵蓋：
顧客掃碼點餐、廚房/吧台即時顯示、服務生結帳、訂位管理、後台報表。

---

## 環境需求

| 工具 | 版本 |
|------|------|
| Java | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 18+ |

---

## 快速啟動

### 1. 建立資料庫

```sql
CREATE DATABASE pobar CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

執行初始化 schema：

```bash
mysql -u root -p pobar < sql/schema.sql
```

### 2. 設定後端環境

複製並修改設定檔：

```bash
cp src/main/resources/application.properties src/main/resources/application-local.properties
```

編輯 `application-local.properties`，至少修改：

```properties
spring.datasource.password=你的資料庫密碼

# 首次啟動自動建立管理員帳號（啟動後請移除此行）
app.init-admin.password=your_admin_password

# JWT 密鑰（請換成 64 字元以上的隨機字串）
jwt.secret=CHANGE_THIS_TO_A_RANDOM_64_CHAR_STRING
```

### 3. 啟動後端

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

後端啟動於 `http://localhost:8080`

### 4. 安裝前端依賴

```bash
cd frontend
npm install
```

### 5. 啟動前端

```bash
npm run dev
```

前端啟動於 `http://localhost:5173`

---

## 頁面對應

| URL | 頁面 | 角色 |
|-----|------|------|
| `/login` | 登入 | 所有人 |
| `/order/:token` | 顧客掃碼點餐 | 公開（QR token 驗證） |
| `/kitchen` | 廚房顯示 | KITCHEN / MANAGER / ADMIN |
| `/bar` | 吧台顯示 | BARTENDER / MANAGER / ADMIN |
| `/staff` | 服務生（桌位+結帳+訂位） | WAITER / MANAGER / ADMIN |
| `/admin` | 後台管理（報表/設定/員工） | ADMIN |

---

## 角色說明

| 角色 | 說明 |
|------|------|
| `ADMIN` | 完整權限 |
| `MANAGER` | 等同 ADMIN，不含使用者管理 |
| `WAITER` | 桌位開關、結帳、訂位管理 |
| `BARTENDER` | 吧台顯示頁（飲品單） |
| `KITCHEN` | 廚房顯示頁（食物單） |

---

## 系統設定（後台可調整）

登入後台 → 系統設定，可修改：

| Key | 預設值 | 說明 |
|-----|--------|------|
| `service_charge_rate` | `0.10` | 服務費率（10%） |
| `frontend_base_url` | `http://localhost:5173` | QR code 內嵌的前端網址 |

---

## ECPay 電子發票串接

目前為 stub 實作。正式上線前，在 `application.properties` 加入：

```properties
ecpay.merchant-id=你的特店編號
ecpay.hash-key=你的HashKey
ecpay.hash-iv=你的HashIV
ecpay.invoice.api-url=https://einvoice.ecpay.com.tw/B2CInvoice/Issue
```

並在 `EcpayInvoiceServiceImpl.java` 實作 `issue()` 方法（參考 ECPay 電子發票 API 文件）。

---

## 本地開發注意事項

- **業務日**從每天 04:00 開始計算（跨夜營業）
- **自動 NO_SHOW**：訂位時間超過 10 分鐘未更新狀態，每分鐘自動標記
- **每日備份**：03:00 自動執行 mysqldump，輸出到 `./backups/`
- **購物車**為 in-memory 儲存（重啟後清空），多手機透過 WebSocket 同步

---

## 專案結構

```
pobar2.0/
├── src/main/java/com/pobar/
│   ├── controller/      # REST 端點（只呼叫 service）
│   ├── service/         # 業務邏輯介面
│   │   └── impl/        # 實作
│   ├── mapper/          # MyBatis Plus mapper
│   ├── entity/          # DB 對應實體
│   ├── dto/             # 請求/回應 DTO
│   ├── security/        # JWT、Security Config、Rate Limit
│   ├── logging/         # Audit AOP、Request log
│   ├── scheduler/       # 定時任務（訂位、備份）
│   ├── config/          # MyBatis Plus、WebSocket config
│   └── util/            # QR code、XSS 工具
├── sql/
│   └── schema.sql       # 資料庫初始化 DDL
├── frontend/            # Vue 3 前端
│   └── src/
│       ├── views/       # 頁面元件
│       ├── stores/      # Pinia 狀態（auth、cart）
│       ├── api/         # Axios instance
│       ├── router/      # Vue Router
│       └── composables/ # WebSocket composable
└── README.md
```
