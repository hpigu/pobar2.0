# 錯誤碼對照表

所有 API 統一使用 `Result<T>` 物件包裝回應，其中 `code` 欄位對應本表。

```json
{
  "code": 1001,
  "message": "帳號或密碼錯誤",
  "data": null
}
```

對應 Java 常數定義：[`com.pobar.common.ErrorCode`](../src/main/java/com/pobar/common/ErrorCode.java)

---

## 編碼規則

| 區段 | 範圍 | 用途 |
|---|---|---|
| 成功 | `200` | 唯一成功碼 |
| HTTP 標準 | `400-599` | 通用錯誤（盡量少用，優先用業務碼） |
| 認證 / 授權 | `1000-1099` | 登入、token、密碼、角色 |
| 使用者 | `1100-1199` | 帳號管理 |
| 商品 / 菜單 | `1200-1299` | Product / Category / Recipe |
| 訂單 | `1300-1399` | Order / Cart |
| 桌位 | `1400-1499` | Table / Session |
| 預約 | `1500-1599` | Reservation |
| 支付 / 發票 | `1600-1699` | Payment / Invoice / ECPay |
| 食材 / 庫存 | `1700-1799` | Ingredient |
| 報表 | `1800-1899` | Report |
| 系統 / 備份 | `1900-1999` | Setting / Backup / File |

---

## 通用 / HTTP

| Code | 常數 | 說明 | 建議前端動作 |
|---:|---|---|---|
| 200 | `SUCCESS` | 成功 | — |
| 400 | `BAD_REQUEST` | 參數錯誤 | toast 顯示 message |
| 401 | `UNAUTHORIZED` | 未登入或 token 失效 | 跳轉登入頁 |
| 403 | `FORBIDDEN` | 無權限 | toast「權限不足」 |
| 404 | `NOT_FOUND` | 資源不存在 | toast 或導頁 |
| 405 | `METHOD_NOT_ALLOWED` | HTTP 方法不被允許 | toast |
| 409 | `CONFLICT` | 資料衝突 | toast |
| 429 | `TOO_MANY_REQUESTS` | 請求過於頻繁 | toast「請稍後再試」 |
| 500 | `INTERNAL_ERROR` | 系統錯誤 | toast「系統錯誤，請聯絡管理員」 |

## 認證 / 授權（1000-1099）

| Code | 常數 | 說明 | 建議前端動作 |
|---:|---|---|---|
| 1001 | `INVALID_CREDENTIALS` | 帳號或密碼錯誤 | toast |
| 1002 | `ACCOUNT_LOCKED` | 帳號被鎖（多次失敗） | toast「請 15 分後再試」 |
| 1003 | `IP_LOCKED` | 來源 IP 被鎖 | toast「來源異常，請稍後」 |
| 1004 | `TOKEN_EXPIRED` | JWT 過期 | 清除本地 token、跳登入 |
| 1005 | `TOKEN_INVALID` | JWT 無效或已撤銷 | 同上 |
| 1006 | `PASSWORD_MUST_CHANGE` | 首次登入需強制改密碼 | 跳改密碼頁 |
| 1007 | `ACCOUNT_INACTIVE` | 帳號被停用 | toast |
| 1008 | `OLD_PASSWORD_WRONG` | 改密碼時舊密碼錯誤 | toast |
| 1009 | `NEW_PASSWORD_WEAK` | 新密碼強度不足 | toast，並顯示規則 |

## 使用者（1100-1199）

| Code | 常數 | 說明 |
|---:|---|---|
| 1101 | `USER_NOT_FOUND` | 使用者不存在 |
| 1102 | `USER_ACCOUNT_EXISTS` | 帳號已存在 |
| 1103 | `USER_EMAIL_EXISTS` | Email 已被註冊 |
| 1104 | `CANNOT_DELETE_SELF` | 不能刪除自己 |
| 1105 | `CANNOT_MODIFY_OWN_ROLE` | 不能修改自己的角色 |

## 商品 / 菜單（1200-1299）

| Code | 常數 | 說明 |
|---:|---|---|
| 1201 | `PRODUCT_NOT_FOUND` | 商品不存在 |
| 1202 | `CATEGORY_NOT_FOUND` | 分類不存在 |
| 1203 | `RECIPE_NOT_FOUND` | 配方不存在 |
| 1204 | `PRODUCT_UNAVAILABLE` | 商品已下架 / 缺料 |
| 1205 | `CATEGORY_IN_USE` | 分類底下尚有商品，不可刪 |

## 訂單（1300-1399）

| Code | 常數 | 說明 |
|---:|---|---|
| 1301 | `ORDER_NOT_FOUND` | 訂單不存在 |
| 1302 | `ORDER_STATUS_INVALID` | 訂單狀態不允許此操作 |
| 1303 | `CART_EMPTY` | 購物車為空 |
| 1304 | `CART_ITEM_NOT_FOUND` | 購物車項目不存在 |

## 桌位（1400-1499）

| Code | 常數 | 說明 |
|---:|---|---|
| 1401 | `TABLE_NOT_FOUND` | 桌位不存在 |
| 1402 | `TABLE_OCCUPIED` | 桌位已被佔用 |
| 1403 | `SESSION_NOT_FOUND` | Session 不存在 |
| 1404 | `SESSION_EXPIRED` | Session 已過期 |

## 預約（1500-1599）

| Code | 常數 | 說明 |
|---:|---|---|
| 1501 | `RESERVATION_NOT_FOUND` | 預約不存在 |
| 1502 | `RESERVATION_SLOT_FULL` | 時段已滿 |
| 1503 | `RESERVATION_TIME_INVALID` | 預約時間不合法 |
| 1504 | `RESERVATION_ALREADY_CANCEL` | 預約已取消 |

## 支付 / 發票（1600-1699）

| Code | 常數 | 說明 |
|---:|---|---|
| 1601 | `PAYMENT_FAILED` | 支付失敗 |
| 1602 | `PAYMENT_NOT_FOUND` | 支付紀錄不存在 |
| 1603 | `INVOICE_FAILED` | 開立發票失敗 |
| 1604 | `ECPAY_NOT_CONFIGURED` | ECPay 尚未設定 |
| 1605 | `AMOUNT_MISMATCH` | 金額不符 |

## 食材 / 庫存（1700-1799）

| Code | 常數 | 說明 |
|---:|---|---|
| 1701 | `INGREDIENT_NOT_FOUND` | 食材不存在 |
| 1702 | `INGREDIENT_IN_USE` | 食材使用中，不可刪 |
| 1703 | `STOCK_INSUFFICIENT` | 庫存不足 |

## 報表（1800-1899）

| Code | 常數 | 說明 |
|---:|---|---|
| 1801 | `REPORT_RANGE_INVALID` | 報表區間錯誤 |
| 1802 | `REPORT_EXPORT_FAILED` | 報表匯出失敗 |

## 系統 / 備份（1900-1999）

| Code | 常數 | 說明 |
|---:|---|---|
| 1901 | `SETTING_NOT_FOUND` | 系統設定不存在 |
| 1902 | `BACKUP_FAILED` | 備份失敗 |
| 1903 | `FILE_UPLOAD_FAILED` | 檔案上傳失敗 |
| 1904 | `FILE_TOO_LARGE` | 檔案過大 |
| 1905 | `FILE_TYPE_NOT_ALLOWED` | 不支援的檔案格式 |
| 1906 | `STORAGE_NOT_CONFIGURED` | 儲存服務未設定（如 S3） |

---

## 前端統一處理範例（Vue 3）

```javascript
// src/utils/request.js
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const api = axios.create({ baseURL: '/api' })

api.interceptors.response.use(
  res => {
    const { code, message, data } = res.data
    if (code === 200) return data
    // 統一處理
    switch (code) {
      case 401:
      case 1004:
      case 1005:
        localStorage.removeItem('token')
        router.push('/login')
        break
      case 1006:
        router.push('/change-password')
        break
      default:
        // 顯示錯誤訊息
        window.$message?.error(message)
    }
    return Promise.reject(res.data)
  }
)
```
