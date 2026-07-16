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
 * 以 UI 走登入流程（給錄影用；驗證登入頁本身也能運作）。
 * 登入後前端會依角色自動導向對應頁面（WAITER→/staff、KITCHEN→/kitchen…）。
 * @param {import('@playwright/test').Page} page
 * @param {string} account
 */
export async function uiLogin(page, account, password = PASSWORD) {
  await page.goto('/login')
  await typeSlowly(page.locator('input[placeholder="請輸入帳號"]'), account)
  await typeSlowly(page.locator('input[placeholder="請輸入密碼"]'), password)
  await clickVisibly(page.getByRole('button', { name: '登入' }))
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
 * 以顧客身分（X-Session-Token）幫指定 session 加一筆點餐項目。
 * 供服務生結帳測試準備「桌內有品項」的真實情境，避免結帳金額為 0。
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} qrToken
 * @param {number} productId
 * @param {number} quantity
 */
export async function addOrderItem(request, qrToken, productId = 1, quantity = 1) {
  const res = await request.post(`${BASE}/api/orders`, {
    headers: { 'X-Session-Token': qrToken },
    data: { items: [{ productId, quantity }] },
  })
  if (!res.ok()) {
    throw new Error(`加點失敗: HTTP ${res.status()} ${await res.text()}`)
  }
}

/**
 * 建立一筆訂位（供服務生「入座」測試前置）。reservedAt 為明天中午，
 * 避開時段/容量驗證的邊界情況。
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {{customerName?:string, customerPhone?:string, partySize?:number}} opts
 * @returns {Promise<{id:number, bookingCode:string}>}
 */
export async function createReservation(request, opts = {}) {
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
  // 營業時間 17:00–21:00（見 ReservationServiceImpl 驗證訊息），取中段 18:00 較保險
  const reservedAt = `${tomorrow.toISOString().slice(0, 10)}T18:00:00`
  const res = await request.post(`${BASE}/api/reservations`, {
    data: {
      customerName: opts.customerName || `測試訂位-${Date.now().toString().slice(-6)}`,
      customerPhone: opts.customerPhone || '0912345678',
      partySize: opts.partySize || 2,
      seatType: 'REGULAR',
      reservedAt,
    },
  })
  if (!res.ok()) {
    throw new Error(`建立訂位失敗: HTTP ${res.status()} ${await res.text()}`)
  }
  const body = await res.json()
  return body.data
}

/**
 * 關閉所有目前 OPEN 的桌位 session（測試前後重置，確保開桌起點乾淨）。
 * @param {import('@playwright/test').APIRequestContext} request
 */
export async function closeAllOpenSessions(request) {
  const { token } = await apiLogin(request, ACCOUNTS.waiter)
  const res = await request.get(`${BASE}/api/tables`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const tables = (await res.json()).data || []
  for (const t of tables) {
    if (t.status === 'OPEN' && t.currentSessionId) {
      await request.delete(`${BASE}/api/tables/sessions/${t.currentSessionId}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
    }
  }
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

  // 先等元素可見（避免對還沒 render 的元素取座標）
  await locator.waitFor({ state: 'visible' })

  if (slow) {
    const page = locator.page()
    // 視覺效果：把游標可見地移到目標中心 → 停頓，讓觀眾看清要點哪。
    // 取不到 box（如剛彈出的 overlay）就略過移動，仍會點到。
    try {
      await locator.scrollIntoViewIfNeeded({ timeout: 3000 })
      const box = await locator.boundingBox()
      if (box) {
        await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 20 })
        await page.waitForTimeout(600)
      }
    } catch { /* 取不到位置就略過移動軌跡 */ }
  }

  // 實際點擊統一用 locator.click({ force: true })：force 跳過「中心點被
  // 子元素覆蓋」的嚴格預檢（桌位平面圖那種 <div>文字</div> 疊層的自訂
  // 卡片會誤判攔截而逾時），但仍派發真實滑鼠事件，Vue @click 正常觸發。
  await locator.click({ force: true })
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

// 對測試 DB 執行一段 SQL（清理用）。加 --default-character-set=utf8mb4
// 確保中文條件正確比對，失敗僅警告不中斷測試。
function runSql(sql, label = '') {
  try {
    execFileSync('docker', [
      'exec', '-i', 'pobar-mysql',
      'mysql', '--default-character-set=utf8mb4',
      '-upobar', `-p${dbPassword()}`, 'pobar', '-e', sql,
    ], { stdio: ['ignore', 'ignore', 'ignore'] })
  } catch (e) {
    console.warn(`[cleanup] ${label} 失敗:`, e.message)
  }
}

/**
 * 刪除指定手機號的所有訂位（測試自我清理，保持 DB 乾淨）。
 * 訂位無員工刪除 API，故直接透過 docker exec 操作測試 DB。
 * 僅限測試手機號使用，避免誤刪真實資料。
 * @param {string} phone
 */
export function deleteReservationsByPhone(phone) {
  runSql(`DELETE FROM reservation WHERE customer_phone='${phone}';`, `清理訂位(${phone})`)
}

/**
 * 刪除指定中文名稱的測試菜單品項（未被訂單引用時才刪得掉，
 * 刪不掉則停用避免污染菜單）。僅用於測試建立的品項名稱。
 * @param {string} nameZh
 */
export function deleteTestProduct(nameZh) {
  runSql(
    `DELETE FROM product WHERE name_zh='${nameZh}';` +
    `UPDATE product SET is_active=0 WHERE name_zh='${nameZh}';`,
    `清理品項(${nameZh})`,
  )
}
