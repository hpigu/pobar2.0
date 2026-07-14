import { defineConfig, devices } from '@playwright/test'

/**
 * E2E 測試設定
 *
 * 測試對象：docker-compose 起的 nginx 前端（http://localhost:80），
 * 後端 / DB 均在 docker 內部網路，由 nginx proxy /api、/uploads、/ws。
 *
 * 錄影：每個測試都輸出 .webm 影片 + trace（逐步截圖），
 * 產物在 test-results/，HTML 報告含可回放的影片與追蹤。
 *
 * 執行：
 *   npx playwright test                 # 全部
 *   npx playwright test customer.spec   # 單一
 *   npx playwright show-report          # 開啟含影片的報告
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,        // 共用同一份 DB，序列執行避免資料互相干擾
  workers: 1,
  retries: 0,
  // 錄影用的「移到目標→停頓→點擊」節奏（clickVisibly）較慢，放寬單測逾時。
  timeout: 120_000,
  reporter: [['html', { open: 'never' }], ['list']],

  use: {
    baseURL: 'http://localhost',
    video: 'on',               // ← 核心：每個測試都錄影
    trace: 'on',               // 逐步操作追蹤（含截圖、DOM 快照）
    screenshot: 'only-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 15_000,
    // 註：不再用全域 slowMo（會與 clickVisibly 的停頓疊加、節奏難預測）。
    // 錄影節奏統一由 helpers 的 clickVisibly / typeSlowly 明確控制，
    // 並以環境變數 SLOWMO 開關（SLOWMO=0 關閉停頓，供純回歸驗證加速）。
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
