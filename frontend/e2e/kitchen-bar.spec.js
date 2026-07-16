import { test, expect } from '@playwright/test'
import {
  uiLogin, clickVisibly, apiLogin, ACCOUNTS, BASE,
  openTableSession, addOrderItem, closeAllOpenSessions,
} from './helpers.js'
import { installCursor } from './cursor.js'

/**
 * 廚房（K2）與吧台（B2）出餐看板
 *
 * 兩者共用 /api/orders/display，廚房 ?type=FOOD、吧台 ?type=DRINK。
 * 前置以 API 開桌並下單，讓看板有品項可操作，再走「開始製作→完成」。
 * 測試前後清空 OPEN session 保持乾淨。
 */

// 取 FOOD 測試品項的 id（sql/test-seed.sql 種入的「測試餐點」）
async function foodProductId(request) {
  const res = await request.get(`${BASE}/api/menu`)
  const items = (await res.json()).data || []
  const food = items.find(p => p.type === 'FOOD')
  if (!food) throw new Error('找不到 FOOD 品項，請先匯入 sql/test-seed.sql')
  return food.id
}

test.describe('廚房出餐看板', () => {
  test.beforeEach(async ({ request }) => { await closeAllOpenSessions(request) })
  test.afterEach(async ({ request }) => { await closeAllOpenSessions(request) })

  test('待製作 → 開始製作 → 完成', async ({ page, request }) => {
    // 前置：開桌 + 下一筆 FOOD 訂單
    const { qrToken } = await openTableSession(request, 2)
    const foodId = await foodProductId(request)
    await addOrderItem(request, qrToken, foodId, 1)

    await installCursor(page)
    await uiLogin(page, ACCOUNTS.kitchen)
    await expect(page).toHaveURL(/\/kitchen/)
    await expect(page.getByText('🍳 廚房顯示')).toBeVisible()

    // 出餐卡片出現「測試餐點」，狀態待製作
    const card = page.locator('.order-card').filter({ hasText: '測試餐點' }).first()
    await expect(card).toBeVisible()

    // 開始製作
    await clickVisibly(card.getByRole('button', { name: '開始製作' }))
    await expect(card.getByRole('button', { name: '完成' })).toBeVisible()

    // 完成 → 卡片離開看板（或狀態更新）
    await clickVisibly(card.getByRole('button', { name: '完成' }))
    // 完成後該品項不再是待處理，卡片消失
    await expect(page.locator('.order-card').filter({ hasText: '測試餐點' })).toHaveCount(0)
  })
})

test.describe('吧台出餐看板', () => {
  test.beforeEach(async ({ request }) => { await closeAllOpenSessions(request) })
  test.afterEach(async ({ request }) => { await closeAllOpenSessions(request) })

  test('DRINK 品項 → 完成', async ({ page, request }) => {
    // 前置：開桌 + 下一筆 DRINK 訂單（琴費士 id=1）
    const { qrToken } = await openTableSession(request, 2)
    await addOrderItem(request, qrToken, 1, 1)

    await installCursor(page)
    await uiLogin(page, ACCOUNTS.bartender)
    await expect(page).toHaveURL(/\/bar/)
    await expect(page.getByText('🍹 吧台顯示')).toBeVisible()

    const card = page.locator('.order-card').filter({ hasText: '琴費士' }).first()
    await expect(card).toBeVisible()

    // 吧台看板：DRINK 一步到位「完成」
    await clickVisibly(card.getByRole('button', { name: '完成' }))
    await expect(page.locator('.order-card').filter({ hasText: '琴費士' })).toHaveCount(0)
  })
})
