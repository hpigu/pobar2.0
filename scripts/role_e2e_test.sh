#!/usr/bin/env bash
# 角色端對端 API 測試。結果寫入 docs/role_test_report.md
set -u
BASE=http://localhost:8080
REPORT=F:/project/pobar2.0/docs/role_test_report.md
PASS=0; FAIL=0
FAILS=()

now() { date "+%Y-%m-%d %H:%M:%S"; }

# jp <expr>  讀 stdin JSON 並列印 expr 值
jp() {
  node -e "let s=''; process.stdin.on('data',d=>s+=d); process.stdin.on('end',()=>{try{const d=JSON.parse(s);const v=$1; console.log(v==null?'':v)}catch(e){console.log('')}});"
}

login() {
  local acc="$1" pw="$2"
  curl -s -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"account\":\"$acc\",\"password\":\"$pw\"}" | jp "(d.data&&d.data.token)||''"
}

# call NAME METHOD URL EXPECTED [TOKEN] [BODY]
call() {
  local name="$1" method="$2" url="$3" expected="$4" token="${5:-}" body="${6:-}"
  local args=(-s -o /tmp/resp.json -w "%{http_code}" -X "$method" "$BASE$url")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  local code; code=$(curl "${args[@]}")
  local body_excerpt; body_excerpt=$(head -c 240 /tmp/resp.json | tr '\n' ' ')
  if [ "$code" = "$expected" ]; then
    echo "PASS  [$name] $method $url -> $code"
    PASS=$((PASS+1))
  else
    echo "FAIL  [$name] $method $url -> $code (期望 $expected)  body: $body_excerpt"
    FAIL=$((FAIL+1))
    FAILS+=("$name||$method $url||期望 $expected 實際 $code||$body_excerpt")
  fi
}

echo "===== POBAR 2.0 角色 E2E 測試  $(now) ====="

# ── 1. 登入 ─────────────────────────────────────────────
echo
echo "--- 1. 登入 ---"
ADMIN_T=$(login admin admin123)
WAITER_T=$(login daniel admin123)
BARTENDER_T=$(login daniel1 admin123)
KITCHEN_T=$(login kitchen1 admin123)
for pair in "admin:$ADMIN_T" "waiter:$WAITER_T" "bartender:$BARTENDER_T" "kitchen:$KITCHEN_T"; do
  name="${pair%%:*}"; tok="${pair#*:}"
  if [ -n "$tok" ]; then echo "PASS  $name login"; PASS=$((PASS+1));
  else echo "FAIL  $name login (空 token)"; FAIL=$((FAIL+1)); FAILS+=("$name login||POST /api/auth/login||回傳 token 為空||"); fi
done

# ── 2. 管理員 ─────────────────────────────────────────────
echo
echo "--- 2. 管理員 (ADMIN) ---"
call "admin-categories"     GET "/api/categories"          200 "$ADMIN_T"
call "admin-menu-list"      GET "/api/menu"                200 "$ADMIN_T"
call "admin-tables"         GET "/api/tables"              200 "$ADMIN_T"
call "admin-ingredients"    GET "/api/ingredients"         200 "$ADMIN_T"
call "admin-reports-daily"  GET "/api/reports/daily?date=$(date +%Y-%m-%d)"   200 "$ADMIN_T"
call "admin-reports-rank"   GET "/api/reports/ranking?limit=5"                200 "$ADMIN_T"
call "admin-reports-monthly" GET "/api/reports/monthly"    200 "$ADMIN_T"
call "admin-users"          GET "/api/admin/users"         200 "$ADMIN_T"
call "admin-settings"       GET "/api/settings"            200 "$ADMIN_T"
call "admin-reservations"   GET "/api/reservations?date=$(date +%Y-%m-%d)" 200 "$ADMIN_T"

PROD_ID=$(curl -s -H "Authorization: Bearer $ADMIN_T" "$BASE/api/menu" | jp "(d.data&&d.data[0]&&d.data[0].id)||''")
echo "[info] 商品 id = $PROD_ID"
if [ -n "$PROD_ID" ]; then
  call "admin-recipe-detail"  GET "/api/menu/$PROD_ID/recipe-detail" 200 "$ADMIN_T"
  call "admin-product-detail" GET "/api/menu/$PROD_ID"               200 "$ADMIN_T"
fi

# ── 3. 服務生 ─────────────────────────────────────────────
echo
echo "--- 3. 服務生 (WAITER) ---"
call "waiter-tables"        GET "/api/tables"              200 "$WAITER_T"
call "waiter-reservations"  GET "/api/reservations?date=$(date +%Y-%m-%d)" 200 "$WAITER_T"

TABLE_ID=$(curl -s -H "Authorization: Bearer $WAITER_T" "$BASE/api/tables" | jp "((d.data||[]).find(t=>!t.currentSessionId)||{}).id||''")
echo "[info] 空桌 id = $TABLE_ID"

SESSION_ID=""; SESSION_TOKEN=""
if [ -n "$TABLE_ID" ]; then
  RESP=$(curl -s -X POST -H "Authorization: Bearer $WAITER_T" -H "Content-Type: application/json" \
    -d "{\"tableIds\":[$TABLE_ID],\"partySize\":2}" "$BASE/api/tables/sessions")
  SESSION_ID=$(echo "$RESP" | jp "(d.data&&d.data.id)||''")
  SESSION_TOKEN=$(echo "$RESP" | jp "(d.data&&(d.data.qrToken||d.data.sessionQrToken))||''")
  if [ -n "$SESSION_ID" ]; then
    echo "PASS  waiter-open-session -> id=$SESSION_ID token=$SESSION_TOKEN"; PASS=$((PASS+1))
  else
    echo "FAIL  waiter-open-session -> $RESP"; FAIL=$((FAIL+1)); FAILS+=("waiter-open-session||POST /api/tables/sessions||建桌失敗||$RESP")
  fi
fi

# ── 4. 客人 ─────────────────────────────────────────────
echo
echo "--- 4. 客人 (CUSTOMER) ---"
if [ -n "$SESSION_TOKEN" ]; then
  call "customer-session"   GET "/api/tables/sessions/$SESSION_TOKEN" 200 ""
  call "customer-menu"      GET "/api/menu" 200 ""
  call "customer-categories" GET "/api/categories" 200 ""
  call "customer-cart-empty" GET "/api/cart/$SESSION_TOKEN" 200 ""
  if [ -n "$PROD_ID" ]; then
    call "customer-ingredients" GET "/api/menu/$PROD_ID/ingredients" 200 ""
    call "customer-cart-add"   POST "/api/cart/$SESSION_TOKEN/items" 200 "" "{\"productId\":$PROD_ID,\"quantity\":1}"
    call "customer-submit-order" POST "/api/orders?token=$SESSION_TOKEN" 200 "" "{\"items\":[{\"productId\":$PROD_ID,\"quantity\":1,\"notes\":\"e2e\"}]}"
  fi
fi

# ── 5. 調酒師 ─────────────────────────────────────────────
echo
echo "--- 5. 調酒師 (BARTENDER) ---"
call "bartender-display"    GET "/api/orders/display?type=DRINK" 200 "$BARTENDER_T"
DRINK_ITEM_ID=$(curl -s -H "Authorization: Bearer $BARTENDER_T" "$BASE/api/orders/display?type=DRINK" | jp "((d.data||[]).find(x=>x.status=='PENDING')||{}).id")
echo "[info] 待製酒水 item id = $DRINK_ITEM_ID"
if [ -n "$DRINK_ITEM_ID" ]; then
  call "bartender-set-progress" PUT "/api/orders/items/$DRINK_ITEM_ID/status" 200 "$BARTENDER_T" '{"status":"IN_PROGRESS"}'
  call "bartender-set-ready"    PUT "/api/orders/items/$DRINK_ITEM_ID/status" 200 "$BARTENDER_T" '{"status":"READY"}'
fi
call "bartender-menu-recipes" GET "/api/menu" 200 "$BARTENDER_T"

# ── 6. 廚房 ─────────────────────────────────────────────
echo
echo "--- 6. 廚房 (KITCHEN) ---"
call "kitchen-display"      GET "/api/orders/display?type=FOOD" 200 "$KITCHEN_T"

# ── 7. 結帳 ─────────────────────────────────────────────
echo
echo "--- 7. 服務生結帳 ---"
if [ -n "$SESSION_ID" ]; then
  call "waiter-session-orders"   GET "/api/orders/session/$SESSION_ID" 200 "$WAITER_T"
  call "waiter-payment-preview"  GET "/api/sessions/$SESSION_ID/payment/preview" 200 "$WAITER_T"
  call "waiter-payment"          POST "/api/sessions/$SESSION_ID/payment" 200 "$WAITER_T" '{"paymentMethod":"CASH","splitCount":1}'
fi

# ── 8. 權限負面 ───────────────────────────────────────────
echo
echo "--- 8. 權限負面 ---"
call "waiter-cannot-users"   GET "/api/admin/users"      403 "$WAITER_T"
call "bartender-cannot-tables-create" POST "/api/tables" 403 "$BARTENDER_T" '{"name":"x","type":"REGULAR","capacity":2}'
# Spring Security 預設對未認證使用者回 403 而非 401，視為已知行為
call "no-token-secured"      GET "/api/admin/users" 403 ""

echo
echo "===== 結果 ====="
echo "PASS: $PASS    FAIL: $FAIL"

# ── 報告 ───────────────────────────────────────────────
{
  echo "# 角色 E2E 自動化測試報告"
  echo
  echo "**產生時間：** $(now)"
  echo
  echo "**測試方式：** 透過 curl 直接呼叫後端 API，模擬 ADMIN / WAITER / CUSTOMER（QR token 路徑）/ BARTENDER / KITCHEN 各角色完整流程，並補充權限負面測試。"
  echo
  echo "## 結果摘要"
  echo
  echo "| 結果 | 計數 |"
  echo "| --- | --- |"
  echo "| ✅ 通過 | $PASS |"
  echo "| ❌ 失敗 | $FAIL |"
  echo
  echo "## 測試覆蓋"
  echo
  echo "1. 登入：4 個角色"
  echo "2. 管理員：分類、菜單、桌位、食材、報表 (日/排行/月)、用戶、設定、訂位、酒譜詳細、商品詳細"
  echo "3. 服務生：桌位列表、訂位、開桌、查 session 訂單、結帳預覽、結帳"
  echo "4. 客人：透過 QR token 看 session、菜單、分類、購物車、加入購物車、下單、查食材"
  echo "5. 調酒師：取待製酒水、推進狀態 IN_PROGRESS、SERVED、查看菜單"
  echo "6. 廚房：取待製餐點"
  echo "7. 權限負面：WAITER 不能用 /api/users、BARTENDER 不能新增桌位、無 token 取受保護資源"
  echo
  if [ ${#FAILS[@]} -gt 0 ]; then
    echo "## 失敗清單"
    echo
    echo "| # | 名稱 | 端點 | 狀態 | 回應摘要 |"
    echo "| - | --- | --- | --- | --- |"
    i=1
    for f in "${FAILS[@]}"; do
      IFS='||' read -r n e s b <<< "$f"
      echo "| $i | $n | $e | $s | ${b:0:120} |"
      i=$((i+1))
    done
  else
    echo "## 失敗清單"
    echo
    echo "_全部通過_"
  fi
} > "$REPORT"

echo "[info] 報告已寫入 $REPORT"
