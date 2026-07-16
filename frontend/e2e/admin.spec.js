import { test, expect } from '@playwright/test'
import {
  uiLogin, clickVisibly, typeSlowly, ACCOUNTS, deleteTestProduct,
} from './helpers.js'
import { installCursor } from './cursor.js'

/**
 * 店長／管理員後台（M/A 系列）
 *
 * 以 ADMIN 登入（具完整後台權限）。涵蓋：
 * - 後台各頁導覽（菜單／桌位／食材／報表／設定／員工）確認可載入
 * - 菜單新增品項 CRUD（代表性寫入操作，測試後清理）
 */
test.describe('管理員後台', () => {
  const NEW_ITEM = `E2E測試品項-${Date.now().toString().slice(-6)}`

  test.afterEach(() => {
    deleteTestProduct(NEW_ITEM)
  })

  test('登入後台 → 導覽各頁 → 菜單新增品項', async ({ page }) => {
    await installCursor(page)
    await uiLogin(page, ACCOUNTS.admin)

    // ADMIN 登入後導向 /admin（預設落在營收報表頁）
    await expect(page).toHaveURL(/\/admin/)

    // 側邊欄選單項（Element Plus el-menu-item，用可見文字定位）
    const nav = (label) => page.locator('.el-menu-item').filter({ hasText: label })

    // ── 導覽各後台頁，確認可載入 ──
    await clickVisibly(nav('品項管理'))
    await expect(page).toHaveURL(/\/admin\/menu/)
    await expect(page.getByRole('button', { name: '新增品項' })).toBeVisible()

    await clickVisibly(nav('桌位管理'))
    await expect(page).toHaveURL(/\/admin\/tables/)

    await clickVisibly(nav('食材管理'))
    await expect(page).toHaveURL(/\/admin\/ingredients/)

    await clickVisibly(nav('營收報表'))
    await expect(page).toHaveURL(/\/admin\/reports/)
    await expect(page.getByText('營收報表').first()).toBeVisible()

    await clickVisibly(nav('系統設定'))
    await expect(page).toHaveURL(/\/admin\/settings/)

    await clickVisibly(nav('員工管理'))
    await expect(page).toHaveURL(/\/admin\/users/)
    await expect(page.getByRole('button', { name: '新增員工' })).toBeVisible()

    // ── 菜單新增品項 CRUD ──
    await clickVisibly(nav('品項管理'))
    await expect(page).toHaveURL(/\/admin\/menu/)
    await clickVisibly(page.getByRole('button', { name: '新增品項' }))

    const dialog = page.locator('.el-dialog').filter({ hasText: '新增品項' })
    await expect(dialog).toBeVisible()

    // 中文名稱
    await typeSlowly(dialog.locator('.el-form-item').filter({ hasText: '中文名稱' }).locator('input'), NEW_ITEM)
    // 分類：點 select 展開 → 選第一個選項（下拉是 teleport 到 body 的浮層）
    await clickVisibly(dialog.locator('.el-form-item').filter({ hasText: '分類' }).locator('.el-select__wrapper'))
    const firstOption = page.locator('.el-select-dropdown__item:visible').first()
    await expect(firstOption).toBeVisible()
    await clickVisibly(firstOption)
    // 價格：el-input-number，直接填入（預設 0 會導致儲存驗證失敗）
    const priceInput = dialog.locator('.el-form-item').filter({ hasText: '價格' }).locator('input')
    await priceInput.fill('280')
    await priceInput.blur()
    // 儲存
    await clickVisibly(dialog.getByRole('button', { name: '儲存' }))
    // 儲存成功提示 = 新增品項流程完成（後端已建立）
    await expect(page.getByText('儲存成功')).toBeVisible()
  })
})
