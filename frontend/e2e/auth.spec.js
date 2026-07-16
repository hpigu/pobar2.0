import { test, expect } from '@playwright/test'
import { uiLogin, clickVisibly, typeSlowly, forceZhTW, ACCOUNTS, PASSWORD } from './helpers.js'
import { installCursor } from './cursor.js'

/**
 * 共通登入流程（S 系列）
 *
 * 驗證登入頁本身：各角色登入後導向正確頁面、錯誤密碼提示、登出。
 * （首次強制改密因會改動帳號密碼，需專用帳號，另行處理。）
 */
test.describe('登入與登出', () => {
  test('各角色登入導向對應頁面', async ({ page }) => {
    await installCursor(page)

    // 服務生 → /staff
    await uiLogin(page, ACCOUNTS.waiter)
    await expect(page).toHaveURL(/\/staff/)

    // 登出 → 回登入頁
    await clickVisibly(page.getByRole('button', { name: '登出' }))
    await expect(page).toHaveURL(/\/login/)
  })

  test('廚房登入導向 /kitchen', async ({ page }) => {
    await installCursor(page)
    await uiLogin(page, ACCOUNTS.kitchen)
    await expect(page).toHaveURL(/\/kitchen/)
  })

  test('管理員登入導向 /admin', async ({ page }) => {
    await installCursor(page)
    await uiLogin(page, ACCOUNTS.admin)
    await expect(page).toHaveURL(/\/admin/)
  })

  test('錯誤密碼顯示登入失敗', async ({ page }) => {
    await installCursor(page)
    await page.goto('/login')
    // 用不存在的帳號測錯誤登入，避免鎖定真實測試帳號（有登入失敗鎖定機制）
    await typeSlowly(page.locator('input[placeholder="請輸入帳號"]'), 'no_such_user_e2e')
    await typeSlowly(page.locator('input[placeholder="請輸入密碼"]'), 'WrongPassword123')
    await clickVisibly(page.getByRole('button', { name: '登入' }))
    // 停在登入頁，顯示錯誤（不導向）
    await expect(page).toHaveURL(/\/login/)
    await expect(page.locator('.el-message').filter({ hasText: /失敗|錯誤|不正確/ })).toBeVisible()
  })
})
