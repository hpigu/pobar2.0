// 錄影用「假游標 + 點擊漣漪」注入腳本
//
// Playwright 錄影預設不畫游標，操作看起來像瞬間完成。
// 這段在每個頁面注入一個跟隨滑鼠的圓點，並在點擊時產生漣漪，
// 讓錄影像真人示範。監聽真實的 mousemove / mousedown 事件，
// 因此 Playwright 的每次點擊、移動都會被自然呈現，無需改測試邏輯。
//
// 用法：測試開頭 await installCursor(page)（在 goto 之前）。

/**
 * @param {import('@playwright/test').Page} page
 */
export async function installCursor(page) {
  await page.addInitScript(() => {
    // 避免 SSR 或非瀏覽器環境報錯
    if (typeof window === 'undefined') return

    const init = () => {
      if (document.getElementById('__pw_cursor__')) return

      const style = document.createElement('style')
      style.textContent = `
        #__pw_cursor__ {
          position: fixed; top: 0; left: 0;
          width: 22px; height: 22px;
          margin: -11px 0 0 -11px;
          border-radius: 50%;
          background: rgba(255, 90, 90, 0.35);
          border: 2px solid rgba(255, 70, 70, 0.9);
          box-shadow: 0 0 8px rgba(255, 70, 70, 0.6);
          pointer-events: none;
          z-index: 2147483647;
          transition: transform 0.08s ease-out;
          will-change: transform;
        }
        #__pw_cursor__.__pw_down__ { transform: scale(0.7); }
        .__pw_ripple__ {
          position: fixed;
          border-radius: 50%;
          border: 2px solid rgba(255, 70, 70, 0.9);
          pointer-events: none;
          z-index: 2147483646;
          animation: __pw_ripple_anim__ 0.6s ease-out forwards;
        }
        @keyframes __pw_ripple_anim__ {
          0%   { width: 10px; height: 10px; opacity: 0.9; }
          100% { width: 60px; height: 60px; opacity: 0; }
        }
      `
      document.head.appendChild(style)

      const dot = document.createElement('div')
      dot.id = '__pw_cursor__'
      document.body.appendChild(dot)

      window.addEventListener('mousemove', (e) => {
        dot.style.transform = `translate(${e.clientX}px, ${e.clientY}px)`
      }, true)

      window.addEventListener('mousedown', (e) => {
        dot.classList.add('__pw_down__')
        const r = document.createElement('div')
        r.className = '__pw_ripple__'
        r.style.left = (e.clientX - 5) + 'px'
        r.style.top = (e.clientY - 5) + 'px'
        document.body.appendChild(r)
        setTimeout(() => r.remove(), 600)
      }, true)

      window.addEventListener('mouseup', () => {
        dot.classList.remove('__pw_down__')
      }, true)
    }

    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', init)
    } else {
      init()
    }
  })
}
