import { test, expect } from '@playwright/test'
import { forceZhTW, typeSlowly, clickVisibly, deleteReservationsByPhone } from './helpers.js'
import { installCursor } from './cursor.js'

// 測試共用手機號（清理時據此刪除，勿改為真實號碼）
const TEST_PHONE = '0912345678'

/**
 * 顧客訂位流程（C1 訂位 → C2 查詢 → C3 自助取消）
 *
 * 免登入、不需開桌 token，是完全獨立的顧客流程（/reservation）。
 * 三段串成一個連貫測試：線上訂位拿到代碼 → 用手機+代碼查詢 → 取消。
 *
 * 設計要點：
 * - 不硬編碼日期字串（頁面日期選項由瀏覽器 new Date() 動態生成）；
 *   改為點「日期列第 2 顆 pill」＝明天，避開今天可能已過的時段。
 * - 時段從實際載入的可訂 slot 中挑第一個，不猜時間。
 * - 手機/姓名帶時間戳，避免與歷史資料衝突、確保查得到自己這筆。
 */
test.describe('顧客訂位流程', () => {
  // 每個測試後清掉自己建的訂位，保持 DB 乾淨
  test.afterEach(() => {
    deleteReservationsByPhone(TEST_PHONE)
  })

  test('線上訂位 → 查詢 → 自助取消', async ({ page }) => {
    await forceZhTW(page)
    await installCursor(page)

    // 用唯一姓名避免與既有資料混淆；手機用固定測試號（afterEach 據此清理）
    const phone = TEST_PHONE
    const guestName = `測試客-${Date.now().toString().slice(-6)}`

    await page.goto('/reservation')

    // ── ① 選日期：點日期列第 2 顆 = 明天（避開今天已過時段）──
    const datePills = page.locator('.res-date-pill')
    await expect(datePills.first()).toBeVisible()
    await clickVisibly(datePills.nth(1))

    // ── ② 座位區：一般座位 ──
    await clickVisibly(page.locator('.res-party-btn', { hasText: '一般座位' }))

    // ── ③ 人數：2 位 ──
    await clickVisibly(page.locator('.res-party-btn').filter({ hasText: /^2$/ }))

    // ── ④ 時段：挑第一個可訂（非 full / past）的 slot ──
    const okSlot = page.locator('.res-time-slot:not(.full):not(.past)').first()
    await expect(okSlot).toBeVisible()
    await clickVisibly(okSlot)

    // ── ⑤ 訂位資料：姓名 + 手機 ──
    await typeSlowly(page.locator('input[placeholder="訂位人姓名"]'), guestName)
    await typeSlowly(page.locator('input[placeholder="09xxxxxxxx"]'), phone)

    // ── 確認訂位 ──
    await clickVisibly(page.getByRole('button', { name: /確認訂位/ }))

    // 成功頁：顯示「訂位已成功送出」與訂位代碼
    await expect(page.getByText('訂位已成功送出')).toBeVisible()
    const codeLoc = page.locator('.num', { hasText: /^[A-Z0-9]{8}$/ }).first()
    await expect(codeLoc).toBeVisible()
    const bookingCode = (await codeLoc.innerText()).trim()
    expect(bookingCode).toMatch(/^[A-Z0-9]{8}$/)

    // ── C2 查詢：開查詢 dialog → 填手機+代碼 → 查詢 ──
    await clickVisibly(page.getByRole('button', { name: '查詢我的訂位' }))
    const dialog = page.locator('.sp-modal')
    await expect(dialog).toBeVisible()
    await typeSlowly(dialog.locator('input[placeholder="09xxxxxxxx"]'), phone)
    await typeSlowly(dialog.locator('input[placeholder="8 位英數代碼"]'), bookingCode)
    await clickVisibly(dialog.getByRole('button', { name: /SEARCH/ }))

    // 查得到剛訂的那筆（姓名出現，狀態已確認）
    await expect(dialog.getByText(guestName)).toBeVisible()
    await expect(dialog.getByText('已確認')).toBeVisible()

    // ── C3 取消：點取消訂位 ──
    await clickVisibly(dialog.getByRole('button', { name: '取消訂位' }))
    await expect(page.getByText('訂位已取消')).toBeVisible()
    // 取消後重新查詢，該筆狀態變為已取消
    await expect(dialog.getByText('已取消')).toBeVisible()
  })
})

// 訂位表單驗證（不需完整流程，獨立驗證）
test.describe('顧客訂位 · 表單驗證', () => {
  test('未選日期時段時確認鈕為停用', async ({ page }) => {
    await forceZhTW(page)
    await page.goto('/reservation')
    // 初始未選日期/時段 → 確認訂位鈕停用
    const submitBtn = page.getByRole('button', { name: /確認訂位/ })
    await expect(submitBtn).toBeDisabled()
  })
})
