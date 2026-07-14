import { test, expect } from '@playwright/test'
import { openTableSession, closeTableSession, forceZhTW, typeSlowly } from './helpers.js'
import { installCursor } from './cursor.js'

/**
 * 顧客點餐流程（C4）
 *
 * 依賴鏈：顧客點餐需先有 OPEN 的 table session（QR token），
 * 該 token 只能由員工開桌產生。故本測試前置以 API 開桌拿 token，
 * 再驅動顧客 UI 走完整流程：瀏覽菜單 → 加入購物車（含備註）→ 送出訂單。
 */
test.describe('顧客點餐流程', () => {
  let qrToken
  let sessionId

  test.beforeEach(async ({ request }) => {
    // 前置：員工開桌（自動挑可用的一般桌，2 人）→ 取得顧客點餐 token
    const session = await openTableSession(request, 2)
    qrToken = session.qrToken
    sessionId = session.sessionId
  })

  test.afterEach(async ({ request }) => {
    // 清理：收桌，避免桌位卡在 OPEN 影響後續測試
    if (sessionId) await closeTableSession(request, sessionId)
  })

  test('掃碼進入菜單 → 加入購物車（含備註）→ 送出訂單', async ({ page }) => {
    await forceZhTW(page)
    await installCursor(page)   // 錄影用假游標 + 點擊漣漪

    // 1. 掃碼進入點餐頁
    await page.goto(`/order/${qrToken}`)

    // 頁面有效：不應顯示「QR Code 無效」，且分類列已載入
    await expect(page.getByText('QR Code 無效或已過期')).toHaveCount(0)
    await expect(page.locator('.sp-cat-item').first()).toBeVisible()

    // 2. 點第一個可點的品項，開啟詳情 modal
    const firstItem = page.locator('.sp-item-row:not(.sp-item-unavailable)').first()
    await expect(firstItem).toBeVisible()
    const itemName = await firstItem.locator('.display').first().innerText()
    await firstItem.click()

    // modal 開啟
    const modal = page.locator('.sp-modal')
    await expect(modal).toBeVisible()

    // 3. 填備註（逐字輸入，錄影看得清楚）+ 增加數量到 2
    await typeSlowly(modal.locator('.sp-textarea'), '少冰、不要吸管')
    await modal.locator('.sp-qty-btn', { hasText: '+' }).click()

    // 4. 加入購物車（按鈕文字含「加入 · NT$」）
    await modal.getByRole('button', { name: /加入/ }).click()

    // modal 關閉、購物車 footer 出現
    await expect(page.locator('.sp-cart-footer')).toBeVisible()

    // 5. 開購物車 drawer
    await page.locator('.sp-cart-btn').click()
    const drawer = page.locator('.sp-drawer')
    await expect(drawer).toBeVisible()
    // 購物車內含剛加入的品項與備註
    await expect(drawer.getByText(itemName)).toBeVisible()
    await expect(drawer.getByText('少冰、不要吸管')).toBeVisible()

    // 6. 送出訂單 → 確認框 → 確定
    await drawer.getByRole('button', { name: /送出訂單/ }).click()
    // Element Plus 確認框（元件預設語系為英文，確認鈕為 OK）
    await page.getByRole('button', { name: 'OK' }).click()

    // 7. 成功提示
    await expect(page.getByText('訂單已送出！')).toBeVisible()
  })
})

// 無效 token 不需開桌前置，獨立一個 describe
test.describe('顧客點餐 · 邊界', () => {
  test('無效 token 顯示錯誤頁', async ({ page }) => {
    await forceZhTW(page)
    await page.goto('/order/invalid-token-xxxx')
    await expect(page.getByText('QR Code 無效或已過期')).toBeVisible()
  })
})
