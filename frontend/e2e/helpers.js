// E2E 測試共用輔助
//
// 測試帳號由 sql/test-seed.sql 種入，密碼統一 Test1234!
// 後端 / 前端由 docker-compose 提供（baseURL http://localhost）。

import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

export const BASE = 'http://localhost'
export const PASSWORD = 'Test1234!'

export const ACCOUNTS = {
  admin:     'test_admin',
  manager:   'test_manager',
  waiter:    'test_waiter',
  bartender: 'test_bartender',
  kitchen:   'test_kitchen',
}

/**
 * 以 API 登入取得 JWT token（給需要前置條件的測試直接用）。
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} account
 * @returns {Promise<{token:string, role:string, userId:number}>}
 */
export async function apiLogin(request, account, password = PASSWORD) {
  const res = await request.post(`${BASE}/api/auth/login`, {
    data: { account, password },
  })
  if (!res.ok()) {
    throw new Error(`登入失敗 ${account}: HTTP ${res.status()} ${await res.text()}`)
  }
  const body = await res.json()
  return {
    token: body.data.token,
    role: body.data.role,
    userId: body.data.userId,
  }
}

// seed 的一般桌 id（sql/test-seed.sql：A1~A4）
const REGULAR_TABLE_IDS = [1, 2, 3, 4]

/**
 * 前置：由員工開一桌，回傳顧客點餐所需的 qrToken。
 * 顧客點餐流程的必要依賴——沒有 OPEN session 就無法點餐。
 *
 * 為避免測試之間（或與手動操作）互相佔桌，會依序嘗試 A1~A4，
 * 挑第一張目前沒被佔用的桌開；全部佔用才拋錯。
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {number} partySize
 * @returns {Promise<{qrToken:string, sessionId:number, tableId:number}>}
 */
export async function openTableSession(request, partySize = 2) {
  const { token } = await apiLogin(request, ACCOUNTS.waiter)
  let lastErr = ''
  for (const tableId of REGULAR_TABLE_IDS) {
    const res = await request.post(`${BASE}/api/tables/sessions`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { tableIds: [tableId], partySize },
    })
    if (res.ok()) {
      const body = await res.json()
      return { qrToken: body.data.qrToken, sessionId: body.data.id, tableId }
    }
    lastErr = `HTTP ${res.status()} ${await res.text()}`
    // 桌位使用中就換下一張；其他錯誤直接拋
    if (!lastErr.includes('使用中')) break
  }
  throw new Error(`開桌失敗（A1~A4 皆不可用）: ${lastErr}`)
}

/**
 * 收桌（測試後清理，避免桌位一直卡在 OPEN）。
 */
export async function closeTableSession(request, sessionId) {
  const { token } = await apiLogin(request, ACCOUNTS.waiter)
  await request.delete(`${BASE}/api/tables/sessions/${sessionId}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
}

/**
 * 在頁面載入前把語言鎖成 zh-TW，避免殘留的 localStorage 造成中英不一致。
 * @param {import('@playwright/test').Page} page
 */
export async function forceZhTW(page) {
  await page.addInitScript(() => {
    localStorage.setItem('lang', 'zh-TW')
  })
}

/**
 * 逐字輸入文字（像真人打字），讓錄影看得清楚。
 * 相對於 fill() 的瞬間填入，這裡用 pressSequentially 帶延遲。
 * 想加速時設環境變數 SLOWMO=0，打字延遲也一併歸零。
 * @param {import('@playwright/test').Locator} locator
 * @param {string} text
 */
export async function typeSlowly(locator, text) {
  const delay = Number(process.env.SLOWMO ?? 800) > 0 ? 60 : 0
  await clickVisibly(locator)
  await locator.pressSequentially(text, { delay })
}

/**
 * 可見地點擊：先把游標「移動」到目標中心 → 停頓（讓觀眾看清要點哪）→ 才按下。
 * 取代 locator.click() 的瞬移直點，讓錄影有「移過去 → 停 → 點」的清楚節奏。
 * 設 SLOWMO=0 時停頓歸零、退回一般快速點擊（純回歸驗證用）。
 * @param {import('@playwright/test').Locator} locator
 */
export async function clickVisibly(locator) {
  const slow = Number(process.env.SLOWMO ?? 800) > 0
  const page = locator.page()

  // 取目標位置；彈出對話框等 overlay 元素可能取不到 box / scroll 失敗，
  // 這種情況退回一般 click（仍會有點擊漣漪，只是少了移動軌跡）。
  let box = null
  if (slow) {
    try {
      await locator.scrollIntoViewIfNeeded({ timeout: 3000 })
      box = await locator.boundingBox()
    } catch { box = null }
  }
  if (!box) {
    await locator.click()
    return
  }

  const cx = box.x + box.width / 2
  const cy = box.y + box.height / 2
  // 分段移動 → 游標可見地滑向目標
  await page.mouse.move(cx, cy, { steps: 20 })
  // 按下前停一下，讓觀眾看清楚游標停在哪個元素上
  await page.waitForTimeout(600)
  // 用 locator.click 實際觸發（座標點擊對某些元件事件綁定較不可靠），
  // 游標已在正確位置、漣漪由 mousedown 事件產生。
  await locator.click()
}

// ── 測試資料清理 ─────────────────────────────────────────

// 從專案根的 .env 讀 DB 密碼（與手動 docker exec 一致），讀不到用預設。
function dbPassword() {
  try {
    const here = dirname(fileURLToPath(import.meta.url))
    const envPath = join(here, '..', '..', '.env')
    const m = readFileSync(envPath, 'utf8').match(/^DB_PASSWORD=(.*)$/m)
    if (m && m[1].trim()) return m[1].trim()
  } catch { /* 讀不到就用預設 */ }
  return 'pobar_pass'
}

/**
 * 刪除指定手機號的所有訂位（測試自我清理，保持 DB 乾淨）。
 * 訂位無員工刪除 API，故直接透過 docker exec 操作測試 DB。
 * 僅限測試手機號使用，避免誤刪真實資料。
 * @param {string} phone
 */
export function deleteReservationsByPhone(phone) {
  const sql = `DELETE FROM reservation WHERE customer_phone='${phone}';`
  try {
    execFileSync('docker', [
      'exec', '-i', 'pobar-mysql',
      'mysql', '-upobar', `-p${dbPassword()}`, 'pobar', '-e', sql,
    ], { stdio: ['ignore', 'ignore', 'ignore'] })
  } catch (e) {
    // 清理失敗不讓測試整體失敗，只在 console 提示
    console.warn(`[cleanup] 清理訂位失敗（phone=${phone}）:`, e.message)
  }
}
