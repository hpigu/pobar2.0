# 角色 E2E 自動化測試報告

**產生時間：** 2026-05-07 17:34:46

**測試方式：** 透過 curl 直接呼叫後端 API，模擬 ADMIN / WAITER / CUSTOMER（QR token 路徑）/ BARTENDER / KITCHEN 各角色完整流程，並補充權限負面測試。

## 結果摘要

| 結果 | 計數 |
| --- | --- |
| ✅ 通過 | 37 |
| ❌ 失敗 | 0 |

## 測試覆蓋

1. 登入：4 個角色
2. 管理員：分類、菜單、桌位、食材、報表 (日/排行/月)、用戶、設定、訂位、酒譜詳細、商品詳細
3. 服務生：桌位列表、訂位、開桌、查 session 訂單、結帳預覽、結帳
4. 客人：透過 QR token 看 session、菜單、分類、購物車、加入購物車、下單、查食材
5. 調酒師：取待製酒水、狀態流轉 PENDING → IN_PROGRESS → READY、查看菜單
6. 廚房：取待製餐點
7. 權限負面：WAITER 不能呼叫 /api/admin/users、BARTENDER 不能新增桌位、無 token 取受保護資源（Spring Security 預設回 403）

## 過程中修正之問題

| # | 問題 | 修正 |
| - | --- | --- |
| 1 | `PaymentServiceImpl` 仍以 `List<OrderItem>` 接收 `orderItemMapper.selectBySessionId`（已改為回傳 `List<OrderItemDisplay>`），導致後端無法編譯 | 改用 `List<OrderItemDisplay>` 並直接讀取 `productName`，移除多餘的 `ProductMapper` 名稱對映與相關 imports |
| 2 | 測試初版誤用 `/api/users`，實際路徑為 `/api/admin/users` | 修正測試腳本端點 |
| 3 | 測試初版讓 BARTENDER 把品項從 IN_PROGRESS 推到 SERVED，但業務規則允許的終態是 `READY`（SERVED 由 WAITER 負責） | 改成 PENDING → IN_PROGRESS → READY |

## 失敗清單

_全部通過_
