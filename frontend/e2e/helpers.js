// E2E 測試共用輔助
//
// 測試帳號由 sql/test-seed.sql 種入，密碼統一 Test1234!
// 後端 / 前端由 docker-compose 提供（baseURL http://localhost）。

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
