# 環境設定指南

本專案分三個環境，啟動方式與必要環境變數各異。

| 環境 | Profile | 啟動方式 | 用途 |
|---|---|---|---|
| **Local** | `local` | `mvn spring-boot:run` 或 IDE 直接執行 | 本機開發 |
| **SIT** | `sit` | `docker compose up -d` | Docker 測試環境 |
| **Prod** | `prod` | `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d` | 正式上線 |

---

## Local（本機開發）

無需設定任何環境變數即可啟動，敏感值都有開發用預設。

啟動前提：
- 本機 MySQL 已啟動於 `localhost:3306`
- 已執行 `sql/schema.sql` 建好資料表
- root / `test1234` 或自行修改 `application-local.properties`

### 可選環境變數（覆寫預設）

| 變數 | 預設 | 說明 |
|---|---|---|
| `DB_USER` | `root` | 資料庫使用者 |
| `DB_PASSWORD` | `test1234` | 資料庫密碼 |
| `JWT_SECRET` | (寫死的開發 secret) | JWT 簽章金鑰 |
| `INIT_ADMIN_PASSWORD` | (空) | 設定後首次啟動會建 admin 帳號，首次登入強制改密碼 |

---

## SIT（Docker 測試環境）

### 必填環境變數

| 變數 | 說明 | 範例 |
|---|---|---|
| `JWT_SECRET` | JWT 簽章金鑰（至少 64 字元） | `openssl rand -base64 48` |

### 建議環境變數

| 變數 | 預設 | 說明 |
|---|---|---|
| `DB_USER` | `pobar` | 資料庫使用者（非 root） |
| `DB_PASSWORD` | `pobar_pass` | 資料庫密碼 |
| `MYSQL_ROOT_PASSWORD` | `root_secret` | MySQL root 密碼 |
| `INIT_ADMIN_ACCOUNT` | `admin` | 初始管理員帳號 |
| `INIT_ADMIN_PASSWORD` | (空) | 初始管理員密碼，**首次登入會強制改密碼** |
| `CORS_ALLOWED_ORIGINS` | (空) | 走 nginx 反代時不需設定。若前後端分 domain 才需要 |
| `SPRING_PROFILES_ACTIVE` | `sit` | profile 名稱 |

### ECPay（如需測試發票）

| 變數 | 說明 |
|---|---|
| `ECPAY_MERCHANT_ID` | 綠界測試環境 MerchantID |
| `ECPAY_HASH_KEY` | 綠界 HashKey |
| `ECPAY_HASH_IV` | 綠界 HashIV |
| `ECPAY_INVOICE_API_URL` | 預設沙箱 `https://einvoice-stage.ecpay.com.tw/...` |

### 建立 .env 範例

於專案根目錄建立 `.env`（**不可進版控**）：

```bash
# === SIT .env 範例 ===
JWT_SECRET=請用 openssl rand -base64 48 產生
DB_USER=pobar
DB_PASSWORD=請改成強密碼
MYSQL_ROOT_PASSWORD=請改成強密碼
INIT_ADMIN_PASSWORD=請改成強密碼
CORS_ALLOWED_ORIGINS=https://sit.pobar.example.com

# ECPay（申請後填，未填則發票功能不啟用）
ECPAY_MERCHANT_ID=
ECPAY_HASH_KEY=
ECPAY_HASH_IV=
```

啟動：
```bash
docker compose up -d
docker compose logs -f backend
```

---

## Prod（正式環境）

**所有敏感環境變數都【必填】，缺一啟動失敗（這是刻意設計）。**

### 必填環境變數

| 變數 | 說明 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密碼（強密碼） |
| `DB_USER` | 應用程式專用 DB user（**不可用 root**） |
| `DB_PASSWORD` | 應用程式 DB 密碼 |
| `JWT_SECRET` | JWT 簽章金鑰，至少 64 字元 |
| `CORS_ALLOWED_ORIGINS` | 正式前端網址 |
| `INIT_ADMIN_PASSWORD` | 初始管理員密碼（**首次登入後強制改密碼**） |

### ECPay 正式金鑰

| 變數 | 說明 |
|---|---|
| `ECPAY_MERCHANT_ID` | 綠界正式 MerchantID |
| `ECPAY_HASH_KEY` | 綠界正式 HashKey |
| `ECPAY_HASH_IV` | 綠界正式 HashIV |
| `ECPAY_INVOICE_API_URL` | `https://einvoice.ecpay.com.tw/B2CInvoice/Issue` |

### S3 儲存（未來上 AWS 時填）

| 變數 | 預設 | 說明 |
|---|---|---|
| `STORAGE_TYPE` | `local` | 改 `s3` 切換到 S3 |
| `S3_BUCKET` | (空) | S3 bucket 名稱 |
| `S3_REGION` | `ap-northeast-1` | AWS region |
| `S3_ACCESS_KEY` | (空) | 建議用 IAM Role 取代 |
| `S3_SECRET_KEY` | (空) | 同上 |

### 部署步驟

```bash
# 1. 建立 .env（絕對不要 commit）
cp .env.example .env
vim .env

# 2. 啟動
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 3. 看 log
docker compose logs -f backend

# 4. 首次登入：使用 INIT_ADMIN_PASSWORD 登入後立即改密碼

# 5. 改完密碼後，將 INIT_ADMIN_PASSWORD 從 .env 移除並重啟
```

### Production DB 連線（給開發者用工具查資料）

DB 不對外暴露，請走 **SSH Tunnel**：

```bash
# 在本機建立 tunnel：把遠端 3306 mapping 到本地 13306
ssh -L 13306:localhost:3306 user@your-prod-server

# DBeaver / Navicat 連線設定
Host: 127.0.0.1
Port: 13306
User: pobar
Password: (.env 內的 DB_PASSWORD)
```

---

## 產生強密碼 / Secret

```bash
# JWT_SECRET（64 字元以上）
openssl rand -base64 48

# 一般密碼
openssl rand -base64 24
```

---

## 安全檢查清單（部署 prod 前）

- [ ] `.env` 已加入 `.gitignore`
- [ ] `JWT_SECRET` 為隨機 64 字元以上
- [ ] DB 使用獨立帳號（非 root），且權限僅限本專案的 schema
- [ ] `INIT_ADMIN_PASSWORD` 設定後，首次登入立即改密碼，並從 `.env` 移除
- [ ] MySQL port 完全不對外（用 SSH Tunnel）
- [ ] `CORS_ALLOWED_ORIGINS` 只允許正式 domain
- [ ] ECPay 切換為正式環境 URL
- [ ] 防火牆只開 80 / 443（與 SSH 的 22）
- [ ] HTTPS 已配置（建議用 Cloudflare 或 nginx + Let's Encrypt）

---

## HTTPS 部署選項

### 選項 A：Cloudflare（最簡單，推薦）
1. 申請 domain（GoDaddy / Namecheap 等）
2. 註冊 Cloudflare 免費帳號，把 domain 的 nameserver 改成 Cloudflare
3. 在 Cloudflare DNS 加 A record 指到你的 server IP
4. SSL/TLS → Full (strict) 模式
5. 完成 — 流量自動經 Cloudflare 加密 + WAF + CDN

### 選項 B：nginx + Let's Encrypt
- 在 `docker-compose.prod.yml` 加 certbot service
- 用 `certbot --nginx -d your-domain.com` 自動申請憑證
- 設定 90 天自動續期 cron

### 選項 C：AWS ALB + ACM（上 AWS 後）
- 用 ALB 接 EC2 / ECS
- ACM 申請免費 SSL 憑證掛 ALB
- ALB 把 HTTP 自動 redirect 到 HTTPS
