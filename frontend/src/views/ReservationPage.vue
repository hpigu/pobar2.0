<script setup>
import { ref, computed } from 'vue'
import api from '@/api/axios'
import { ElMessage } from 'element-plus'

const selectedDate = ref(null)   // 'YYYY-MM-DD'
const selectedTime = ref(null)   // 'HH:mm'
const slots = ref([])
const loadingSlots = ref(false)

const form = ref({ customerName: '', customerPhone: '', partySize: 2, notes: '' })
const submitting = ref(false)
const bookingCode = ref('')
const success = ref(false)

// 查詢訂位
const queryDialog = ref(false)
const queryPhone = ref('')
const queryCode = ref('')
const queryLoading = ref(false)
const queryResults = ref([])
const cancellingId = ref(null)
const STATUS_LABEL = { CONFIRMED:'已確認', SEATED:'已入座', CANCELLED:'已取消', AUTO_CANCELLED:'逾時取消', NO_SHOW:'未到場', COMPLETED:'已完成' }

// 生成未來7天日期選項
const dateOptions = computed(() => {
  const days = ['SUN','MON','TUE','WED','THU','FRI','SAT']
  const months = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC']
  const result = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(); d.setDate(d.getDate() + i)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    result.push({
      value: `${yyyy}-${mm}-${dd}`,
      d: d.getDate(),
      m: months[d.getMonth()],
      dow: days[d.getDay()],
    })
  }
  return result
})

async function selectDate(val) {
  selectedDate.value = val
  selectedTime.value = null
  loadingSlots.value = true
  try {
    const res = await api.get(`/api/reservations/slots?date=${val}`)
    slots.value = res.data.data || []
  } catch {
    ElMessage.error('載入時段失敗')
  } finally {
    loadingSlots.value = false
  }
}

function isPastSlot(time) {
  if (!selectedDate.value) return false
  const now = new Date()
  const [h, m] = time.split(':').map(Number)
  const dt = new Date(selectedDate.value); dt.setHours(h, m, 0, 0)
  return dt <= now
}

function selectTime(slot) {
  if (!slot.available || isPastSlot(slot.time)) return
  selectedTime.value = slot.time
}

const reservedAtIso = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return null
  return `${selectedDate.value}T${selectedTime.value}:00`
})

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const opt = dateOptions.value.find(d => d.value === selectedDate.value)
  return opt ? `${opt.m} ${opt.d} · ${opt.dow}` : selectedDate.value
})

async function submit() {
  if (!selectedDate.value)              { ElMessage.warning('請選擇日期'); return }
  if (!selectedTime.value)              { ElMessage.warning('請選擇時段'); return }
  if (!form.value.customerName.trim()) { ElMessage.warning('請填寫姓名'); return }
  if (!/^09\d{8}$/.test(form.value.customerPhone)) { ElMessage.warning('手機格式：09xxxxxxxx'); return }
  submitting.value = true
  try {
    const res = await api.post('/api/reservations', {
      customerName:  form.value.customerName,
      customerPhone: form.value.customerPhone,
      partySize:     form.value.partySize,
      notes:         form.value.notes,
      reservedAt:    reservedAtIso.value,
    })
    bookingCode.value = res.data?.data?.bookingCode || ''
    success.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '訂位失敗，請稍後再試')
  } finally {
    submitting.value = false
  }
}

async function queryMyReservations() {
  if (!/^09\d{8}$/.test(queryPhone.value)) { ElMessage.warning('請輸入正確手機格式：09xxxxxxxx'); return }
  if (!queryCode.value.trim()) { ElMessage.warning('請輸入訂位代碼'); return }
  queryLoading.value = true
  try {
    const res = await api.get('/api/reservations/my', {
      params: { phone: queryPhone.value, code: queryCode.value.trim().toUpperCase() }
    })
    queryResults.value = res.data.data || []
    if (!queryResults.value.length) ElMessage.info('查無符合的訂位')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '查詢失敗')
  } finally {
    queryLoading.value = false
  }
}

function openQueryDialog() {
  queryPhone.value = ''; queryCode.value = ''; queryResults.value = []
  queryDialog.value = true
}

async function cancelReservation(r) {
  cancellingId.value = r.id
  try {
    await api.post('/api/reservations/cancel', {
      phone: queryPhone.value,
      code: queryCode.value.trim().toUpperCase(),
    })
    ElMessage.success('訂位已取消')
    await queryMyReservations()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '取消失敗，請稍後再試')
  } finally {
    cancellingId.value = null
  }
}

function reset() {
  success.value = false; selectedDate.value = null; selectedTime.value = null
  slots.value = []; form.value = { customerName: '', customerPhone: '', partySize: 2, notes: '' }
}
</script>

<template>
  <div class="speak res-page">
    <div class="mist"/><div class="grain"/><div class="vignette"/>

    <!-- ── Success screen ── -->
    <div v-if="success" style="position:relative; z-index:1; min-height:100vh; display:flex; align-items:center; justify-content:center; padding:40px 24px;">
      <div style="text-align:center; max-width:420px;">
        <div class="num" style="font-size:10px; color:var(--gold); letter-spacing:0.4em; margin-bottom:20px;">✦ · CONFIRMED · ✦</div>
        <div class="display" style="font-size:52px; line-height:1; margin-bottom:8px;">
          See you <span class="display-it" style="color:var(--moon);">soon.</span>
        </div>
        <div class="display-it" style="font-size:20px; color:var(--fg-1); margin-bottom:32px;">訂位已成功送出</div>

        <div style="padding:24px 28px; border:1px solid var(--line-strong); margin-bottom:24px; text-align:left;">
          <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:16px;">RESERVATION DETAILS</div>
          <div style="display:grid; gap:10px;">
            <div style="display:flex; justify-content:space-between;">
              <span style="color:var(--fg-2); font-size:13px;">日期時段</span>
              <span class="display" style="font-size:15px;">{{ selectedDateLabel }} {{ selectedTime }}</span>
            </div>
            <div style="display:flex; justify-content:space-between;">
              <span style="color:var(--fg-2); font-size:13px;">訂位人</span>
              <span class="display" style="font-size:15px;">{{ form.customerName }} · {{ form.partySize }} 位</span>
            </div>
          </div>
        </div>

        <div v-if="bookingCode" style="padding:20px 28px; border:1px dashed var(--moon-soft); margin-bottom:24px;">
          <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:10px;">訂位代碼 · 請保留以便查詢</div>
          <div class="num" style="font-size:32px; letter-spacing:6px; color:var(--moon);">{{ bookingCode }}</div>
        </div>

        <div style="font-size:12px; color:var(--fg-2); margin-bottom:28px;">如需取消請提前來電，謝謝</div>

        <div style="display:flex; gap:12px; justify-content:center; flex-wrap:wrap;">
          <button class="sp-btn-ghost" @click="openQueryDialog">查詢我的訂位</button>
          <button class="sp-btn" @click="reset">再訂一次</button>
        </div>
      </div>
    </div>

    <!-- ── Main reservation layout ── -->
    <div v-else style="position:relative; z-index:1; min-height:100vh; display:grid; grid-template-columns:1fr 1.15fr;">

      <!-- ── Left: Atmospheric column ── -->
      <div class="res-left">
        <!-- Logo -->
        <div style="display:inline-flex; align-items:center; gap:10px; margin-bottom:auto;">
          <svg width="24" height="24" viewBox="0 0 32 32" class="logo-breath">
            <defs>
              <radialGradient id="moonG2" cx="40%" cy="40%">
                <stop offset="0%" stop-color="#dfe9f7"/>
                <stop offset="60%" stop-color="#8eb6e8"/>
                <stop offset="100%" stop-color="#3d5e8e"/>
              </radialGradient>
            </defs>
            <circle cx="16" cy="16" r="11" fill="url(#moonG2)" opacity="0.95"/>
            <circle cx="20" cy="13" r="9.5" fill="var(--bg-0)"/>
          </svg>
          <div class="display" style="font-size:20px; letter-spacing:0.06em;">
            Po<span style="font-style:italic; opacity:0.85">bar</span>
          </div>
        </div>

        <!-- Editorial text -->
        <div>
          <div class="num" style="font-size:10px; color:var(--gold); letter-spacing:0.4em; margin-bottom:18px;">
            ✦ · RESERVATIONS · ✦
          </div>
          <div class="display" style="font-size:clamp(44px, 5vw, 68px); line-height:1.05; letter-spacing:0.005em; margin-bottom:24px;">
            Hold the<br/>
            <span class="display-it" style="color:var(--moon);">quiet</span> for me.
          </div>
          <div style="font-size:13px; color:var(--fg-1); line-height:1.85; max-width:340px; margin-bottom:28px;">
            一張臨窗的座位、一杯與你今晚相配的調酒，<br/>
            還有一個被音樂溫柔包覆的兩小時。
          </div>
          <div class="sp-rule" style="width:180px;">OPEN · 19—02</div>
        </div>

        <!-- Footer info -->
        <div>
          <div class="num" style="font-size:10px; color:var(--fg-2); letter-spacing:0.3em; margin-bottom:8px;">FIND US</div>
          <div style="font-size:12px; color:var(--fg-2); line-height:1.7;">台北市大安區 · 永康街 · 巷弄 12 號 2F</div>
          <button class="res-query-link" @click="openQueryDialog">查詢我的訂位 →</button>
        </div>
      </div>

      <!-- ── Right: Form ── -->
      <div class="res-right">

        <!-- ① Date -->
        <div class="res-field-group">
          <div class="res-step-label">① DATE · 選擇日期</div>
          <div class="res-date-strip">
            <div v-for="opt in dateOptions" :key="opt.value"
              class="res-date-pill"
              :class="{ active: selectedDate === opt.value }"
              @click="selectDate(opt.value)">
              <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.2em;">{{ opt.dow }}</div>
              <div class="display" style="font-size:22px; line-height:1.2;"
                :style="{ color: selectedDate === opt.value ? 'var(--moon)' : 'var(--fg-0)' }">
                {{ opt.d }}
              </div>
              <div class="num" style="font-size:8px; color:var(--fg-2); letter-spacing:0.15em;">{{ opt.m }}</div>
            </div>
          </div>
          <div v-if="loadingSlots" class="num" style="font-size:11px; color:var(--fg-2); letter-spacing:0.2em; margin-top:10px;">
            LOADING...
          </div>
        </div>

        <!-- ② Party size -->
        <div class="res-field-group">
          <div class="res-step-label">② PARTY · 用餐人數</div>
          <div style="display:flex; gap:6px;">
            <div v-for="n in [1,2,3,4,5,6]" :key="n"
              class="display res-party-btn"
              :class="{ active: form.partySize === n }"
              @click="form.partySize = n">{{ n }}</div>
          </div>
        </div>

        <!-- ③ Time slots -->
        <div class="res-field-group" v-if="selectedDate">
          <div class="res-step-label">③ TIME · 選擇時段</div>
          <div v-if="slots.length === 0 && !loadingSlots" style="font-size:13px; color:var(--fg-2);">此日期無可用時段</div>
          <div v-else class="res-time-grid">
            <div v-for="slot in slots" :key="slot.time"
              class="num res-time-slot"
              :class="{
                active: selectedTime === slot.time,
                full: !slot.available,
                past: isPastSlot(slot.time),
              }"
              @click="selectTime(slot)">
              {{ slot.time }}
              <span v-if="!slot.available && !isPastSlot(slot.time)"
                style="display:block; font-size:8px; letter-spacing:0.15em; margin-top:2px; color:var(--danger);">FULL</span>
              <span v-else-if="isPastSlot(slot.time)"
                style="display:block; font-size:8px; letter-spacing:0.15em; margin-top:2px;">PAST</span>
            </div>
          </div>
        </div>

        <!-- ④ Guest info -->
        <div class="res-field-group">
          <div class="res-step-label">④ GUEST · 訂位資料</div>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:16px;">
            <div>
              <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:6px;">NAME</div>
              <input v-model="form.customerName" placeholder="訂位人姓名" class="res-input display" maxlength="50"/>
            </div>
            <div>
              <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:6px;">PHONE</div>
              <input v-model="form.customerPhone" placeholder="09xxxxxxxx" class="res-input num" maxlength="10"/>
            </div>
          </div>
          <div style="margin-top:14px;">
            <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:6px;">NOTES (選填)</div>
            <textarea v-model="form.notes" placeholder="過敏、特殊需求等..." class="res-textarea" rows="2" maxlength="200"/>
          </div>
        </div>

        <!-- Summary + Submit -->
        <div v-if="selectedDate && selectedTime" class="res-summary">
          <div>
            <div class="display-it" style="font-size:16px;">
              {{ selectedDateLabel }} · {{ selectedTime }} · {{ form.partySize }} 位
            </div>
            <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.2em; margin-top:4px;">暫定 · 送出後確認</div>
          </div>
          <button class="sp-btn" :disabled="submitting" @click="submit">
            {{ submitting ? '送出中...' : '確認訂位 →' }}
          </button>
        </div>
        <div v-else style="padding:0 0 8px;">
          <button class="sp-btn" style="opacity:0.35; cursor:not-allowed; padding:14px 24px; font-size:11px; letter-spacing:0.28em;" disabled>
            確認訂位 →
          </button>
        </div>
      </div>
    </div>

    <!-- ── Query dialog ── -->
    <Transition name="modal-fade">
      <div v-if="queryDialog" class="sp-modal-overlay" @click.self="queryDialog = false">
        <div class="sp-modal speak" style="padding:28px;">
          <div class="mist" style="position:absolute; opacity:0.5;"/>
          <div style="position:relative; z-index:1;">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
              <div class="display-it" style="font-size:24px;">查詢訂位</div>
              <button class="sp-close-btn" @click="queryDialog = false">×</button>
            </div>
            <div style="font-size:12px; color:var(--fg-2); margin-bottom:20px; line-height:1.6;">
              需同時提供手機號 + 訂位成功時取得的代碼
            </div>

            <div style="display:grid; gap:14px; margin-bottom:18px;">
              <div>
                <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:6px;">PHONE</div>
                <input v-model="queryPhone" placeholder="09xxxxxxxx" class="res-input num" maxlength="10"/>
              </div>
              <div>
                <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:6px;">BOOKING CODE</div>
                <input v-model="queryCode" placeholder="8 位英數代碼" class="res-input num" maxlength="8"
                  style="text-transform:uppercase;" @keyup.enter="queryMyReservations"/>
              </div>
            </div>

            <button class="sp-btn" style="width:100%; padding:14px; font-size:11px;" :disabled="queryLoading" @click="queryMyReservations">
              {{ queryLoading ? 'SEARCHING...' : 'SEARCH →' }}
            </button>

            <div v-if="queryResults.length" style="margin-top:20px; display:grid; gap:12px;">
              <div v-for="r in queryResults" :key="r.id"
                style="padding:16px; border:1px solid var(--line-strong);">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                  <div>
                    <div class="display" style="font-size:16px; margin-bottom:4px;">
                      {{ r.reservedAt?.slice(0,16).replace('T',' ') }}
                    </div>
                    <div style="font-size:12px; color:var(--fg-2);">{{ r.customerName }} · {{ r.partySize }} 位</div>
                    <div v-if="r.notes" style="font-size:11px; color:var(--fg-3); margin-top:4px;">{{ r.notes }}</div>
                  </div>
                  <div class="num" style="font-size:9px; letter-spacing:0.15em;"
                    :style="{ color: r.status === 'CONFIRMED' ? 'var(--moon)' : r.status === 'CANCELLED' ? 'var(--danger)' : 'var(--fg-2)' }">
                    {{ STATUS_LABEL[r.status] || r.status }}
                  </div>
                </div>
                <div v-if="r.status === 'CONFIRMED'" style="margin-top:12px; text-align:right;">
                  <button class="sp-btn-ghost" style="font-size:10px; padding:8px 16px;"
                    :disabled="cancellingId === r.id"
                    @click="cancelReservation(r)">
                    {{ cancellingId === r.id ? '取消中...' : '取消訂位' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.res-page { min-height: 100vh; position: relative; overflow: hidden; }

/* ── Left column ── */
.res-left {
  padding: 48px 44px;
  border-right: 1px solid var(--line);
  display: flex; flex-direction: column;
  justify-content: space-between;
  min-height: 100vh;
  gap: 32px;
}

/* ── Right column ── */
.res-right {
  padding: 44px 44px 60px;
  overflow-y: auto;
  max-height: 100vh;
  display: flex; flex-direction: column; gap: 28px;
}

/* ── Field group ── */
.res-field-group { display: flex; flex-direction: column; gap: 12px; }

/* ── Step label ── */
.res-step-label {
  font-family: var(--font-mono);
  font-size: 10px; color: var(--fg-2);
  letter-spacing: 0.3em; text-transform: uppercase;
}

/* ── Date strip ── */
.res-date-strip {
  display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px;
}
.res-date-pill {
  min-width: 52px; padding: 10px 6px;
  border: 1px solid var(--line-strong);
  text-align: center; cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.res-date-pill.active {
  border-color: var(--moon);
  background: rgba(142,182,232,0.08);
}
.res-date-pill:hover:not(.active) { border-color: var(--fg-3); }

/* ── Party buttons ── */
.res-party-btn {
  flex: 1; padding: 14px 0; text-align: center; font-size: 20px;
  border: 1px solid var(--line); color: var(--fg-1);
  cursor: pointer; transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.res-party-btn.active {
  border-color: var(--moon); color: var(--moon);
  background: rgba(142,182,232,0.06);
}
.res-party-btn:hover:not(.active) { border-color: var(--fg-3); }

/* ── Time grid ── */
.res-time-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px;
}
.res-time-slot {
  padding: 11px 0; text-align: center; font-size: 13px; letter-spacing: 0.08em;
  border: 1px solid var(--line); color: var(--fg-1);
  cursor: pointer; transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.res-time-slot.active {
  border-color: var(--moon); color: var(--moon);
  background: rgba(142,182,232,0.06);
}
.res-time-slot.full, .res-time-slot.past {
  color: var(--fg-3); cursor: not-allowed;
  opacity: 0.5;
}
.res-time-slot:hover:not(.active):not(.full):not(.past) { border-color: var(--fg-3); }

/* ── Inputs ── */
.res-input {
  width: 100%; background: transparent;
  border: none; border-bottom: 1px solid var(--line-strong);
  color: var(--fg-0); font-size: 17px; padding: 0 0 8px;
  outline: none; letter-spacing: 0.02em;
}
.res-input::placeholder { color: var(--fg-3); font-size: 13px; }
.res-input:focus { border-bottom-color: var(--moon); }

.res-textarea {
  width: 100%; background: rgba(255,255,255,0.03);
  border: 1px solid var(--line-strong);
  color: var(--fg-0); font-family: var(--font-body);
  font-size: 13px; padding: 10px 12px; resize: none; outline: none;
}
.res-textarea:focus { border-color: var(--moon); }
.res-textarea::placeholder { color: var(--fg-3); }

/* ── Summary bar ── */
.res-summary {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 18px;
  border: 1px solid var(--line);
  background: rgba(142,182,232,0.04);
  gap: 16px; flex-wrap: wrap;
}

/* ── Query link ── */
.res-query-link {
  background: transparent; border: none;
  color: var(--fg-2); font-family: var(--font-mono);
  font-size: 10px; letter-spacing: 0.25em;
  cursor: pointer; padding: 0; margin-top: 12px;
  text-transform: uppercase; transition: color 0.2s;
}
.res-query-link:hover { color: var(--moon); }

/* ── Modal ── */
.sp-modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(7,16,29,0.85);
  display: flex; align-items: flex-end; justify-content: center;
}
@media (min-width: 640px) {
  .sp-modal-overlay { align-items: center; padding: 20px; }
}
.sp-modal {
  position: relative; width: 100%; max-width: 480px;
  max-height: 90vh; overflow-y: auto;
  border-top: 1px solid var(--line-strong);
}
@media (min-width: 640px) {
  .sp-modal { border: 1px solid var(--line-strong); }
}

.sp-close-btn {
  width: 30px; height: 30px; border-radius: 50%;
  border: 1px solid var(--line-strong); background: transparent;
  color: var(--fg-1); font-size: 16px; cursor: pointer;
  display: grid; place-items: center;
}

/* ── Mobile: stack columns ── */
@media (max-width: 768px) {
  .res-page > div:not(.mist):not(.grain):not(.vignette) {
    grid-template-columns: 1fr !important;
  }
  .res-left {
    min-height: auto; padding: 32px 24px;
    border-right: none; border-bottom: 1px solid var(--line);
  }
  .res-right { padding: 32px 24px; max-height: none; }
}

/* ── Transitions ── */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.25s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
