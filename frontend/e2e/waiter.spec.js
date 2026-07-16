import { test, expect } from '@playwright/test'
import {
  uiLogin, clickVisibly, typeSlowly,
  addOrderItem, createReservation, deleteReservationsByPhone,
  closeAllOpenSessions, ACCOUNTS,
} from './helpers.js'
import { installCursor } from './cursor.js'

/**
 * 服務生流程（W1~W7）
 *
 * 開桌 → 結帳 → 關桌 走一次完整營運循環；訂位入座另開一個測試，
 * 避免兩者互相干擾桌位狀態。
 */
test.describe('服務生 · 開桌到結帳', () => {
  // 確保開桌測試起點無殘留 OPEN session
  test.beforeEach(async ({ request }) => { await closeAllOpenSessions(request) })
  test.afterEach(async ({ request }) => { await closeAllOpenSessions(request) })

  test('開桌 → 桌內加點（API 模擬顧客）→ 結帳 → 關桌', async ({ page, request }) => {
    await installCursor(page)
    await uiLogin(page, ACCOUNTS.waiter)

    // 登入後應導向服務生工作台
    await expect(page).toHaveURL(/\/staff/)
    await expect(page.getByText('服務生工作台')).toBeVisible()

    // ── W2 開桌：點空桌 A1（固定用 A1，seed 保證存在且測試前已清乾淨）──
    const tableName = 'A1'
    // 桌位平面圖為 Element Plus el-card 巢狀 + 絕對定位，DOM 對定位不友善。
    // 用桌名文字節點的最外層匹配（.first()），clickVisibly 以 force 點擊，
    // 點擊會冒泡到卡片外層的 @click 觸發開桌。
    const tableCard = page.getByText(tableName, { exact: true }).first()
    await expect(tableCard).toBeVisible()
    await clickVisibly(tableCard)

    // 開桌 dialog 標題為「開桌 — A1」
    const openDialog = page.locator('.el-dialog').filter({ has: page.getByText(`開桌 — ${tableName}`) })
    await expect(openDialog).toBeVisible()
    await clickVisibly(openDialog.getByRole('button', { name: '開桌' }))

    // 開桌成功 → QR Code dialog 彈出
    const qrDialog = page.locator('.el-dialog').filter({ hasText: '掃碼點餐' })
    await expect(qrDialog).toBeVisible()
    const qrUrl = await qrDialog.locator('div', { hasText: /^http/ }).last().innerText()
    const qrToken = qrUrl.trim().split('/').pop()
    await clickVisibly(qrDialog.getByRole('button', { name: '關閉' }))

    // ── 用 API 模擬顧客加點一杯酒，讓結帳金額非 0 ──
    await addOrderItem(request, qrToken, 1, 2)

    // 重新點桌位，刷新該桌訂單顯示（同上用精確桌名定位）
    await clickVisibly(tableCard)
    await expect(page.getByText(`${tableName} 的訂單`)).toBeVisible()
    // 頁面有兩個 el-table（訂單 + 訂位），用第一個（訂單表）驗證品項
    await expect(page.locator('.el-table').first()).toContainText('琴費士')

    // ── W6 結帳 ──
    await clickVisibly(page.getByRole('button', { name: '結帳' }))
    const checkoutDialog = page.locator('.el-dialog').filter({ hasText: '結帳' })
    await expect(checkoutDialog).toBeVisible()
    await expect(checkoutDialog.getByText('合計')).toBeVisible()
    // 付款方式維持預設現金，直接確認結帳
    await clickVisibly(checkoutDialog.getByRole('button', { name: '確認結帳' }))
    await expect(page.getByText('結帳完成')).toBeVisible()

    // 結帳後 session 關閉，訂單區塊消失（該桌不再是選中的 OPEN 桌）
    await expect(page.getByText(`${tableName} 的訂單`)).toHaveCount(0)
  })
})

test.describe('服務生 · 訂位入座', () => {
  const TEST_PHONE = '0912345678'
  let reservation

  test.beforeEach(async ({ request }) => {
    reservation = await createReservation(request, { customerPhone: TEST_PHONE, partySize: 2 })
  })

  test.afterEach(async ({ request }) => {
    deleteReservationsByPhone(TEST_PHONE)
    await closeAllOpenSessions(request)   // 入座會開桌，測試後關閉
  })

  test('訂位管理：入座並自動開桌', async ({ page }) => {
    await installCursor(page)
    await uiLogin(page, ACCOUNTS.waiter)
    await expect(page).toHaveURL(/\/staff/)

    // 訂位列表應出現剛建立的那筆（今天日期選擇器需切到明天）
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
    await page.locator('.el-date-editor input').fill(tomorrow)
    await page.keyboard.press('Enter')

    const row = page.locator('.el-table__row').filter({ hasText: '2' }).first()
    await expect(row).toBeVisible()

    // ── 入座 ──
    await clickVisibly(row.getByRole('button', { name: '入座' }))
    const seatDialog = page.locator('.el-dialog').filter({ hasText: '入座 —' })
    await expect(seatDialog).toBeVisible()

    // 勾選第一張可用桌
    await clickVisibly(seatDialog.locator('.el-checkbox').first())
    await expect(seatDialog.getByText('容量足夠')).toBeVisible()
    await clickVisibly(seatDialog.getByRole('button', { name: '確認入座並開桌' }))

    // 入座成功 → QR Code dialog（代表已自動開桌）
    await expect(page.getByText('入座成功')).toBeVisible()
    const qrDialog = page.locator('.el-dialog').filter({ hasText: '掃碼點餐' })
    await expect(qrDialog).toBeVisible()
  })
})
