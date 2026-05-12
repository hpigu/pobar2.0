<script setup>
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import api from '@/api/axios'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const dailyDate = ref(new Date().toISOString().split('T')[0])
const dailyData = ref(null)
const rankingData = ref([])
const monthlyData = ref(null)

// 三個區塊的載入 / 錯誤狀態，互不影響
const dailyState   = ref({ loading: false, error: null })
const rankingState = ref({ loading: false, error: null })
const monthlyState = ref({ loading: false, error: null })

async function loadDaily() {
  dailyState.value = { loading: true, error: null }
  try {
    const res = await api.get(`/api/reports/daily?date=${dailyDate.value}`)
    dailyData.value = res.data.data
  } catch (e) {
    dailyState.value.error = e?.response?.data?.message || '載入失敗'
  } finally {
    dailyState.value.loading = false
  }
}

async function loadRanking() {
  rankingState.value = { loading: true, error: null }
  try {
    const res = await api.get('/api/reports/ranking?limit=10')
    rankingData.value = res.data.data?.products || []
  } catch (e) {
    rankingData.value = []
    rankingState.value.error = e?.response?.data?.message || '載入失敗'
  } finally {
    rankingState.value.loading = false
  }
}

async function loadMonthly() {
  monthlyState.value = { loading: true, error: null }
  try {
    const res = await api.get('/api/reports/monthly')
    monthlyData.value = res.data.data
  } catch (e) {
    monthlyState.value.error = e?.response?.data?.message || '載入失敗'
  } finally {
    monthlyState.value.loading = false
  }
}

const hourlyOption = ref({})
const rankingOption = ref({})
const monthlyOption = ref({})

// 空資料時的 placeholder option（讓 echarts 顯示「無資料」標題，而不是整片白）
function emptyOption(title, message = '無資料') {
  return {
    title: {
      text: title,
      subtext: message,
      left: 'center',
      top: 'middle',
      subtextStyle: { color: '#999', fontSize: 14 },
    },
  }
}

function buildHourly() {
  if (!dailyData.value || !dailyData.value.hourly?.length) {
    hourlyOption.value = emptyOption('今日每小時收入', dailyState.value.error || '無資料')
    return
  }
  hourlyOption.value = {
    title: { text: '今日每小時收入' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dailyData.value.hourly.map(h => `${h.hour}時`) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: dailyData.value.hourly.map(h => h.revenue), name: '收入' }],
  }
}

function buildRanking() {
  if (!rankingData.value.length) {
    rankingOption.value = emptyOption('銷售排行 Top10', rankingState.value.error || '尚無銷售資料')
    return
  }
  rankingOption.value = {
    title: { text: '銷售排行 Top10' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: rankingData.value.map(r => r.productName).reverse() },
    series: [{ type: 'bar', data: rankingData.value.map(r => r.totalQuantity).reverse(), name: '銷售量' }],
  }
}

function buildMonthly() {
  if (!monthlyData.value || !monthlyData.value.dates?.length) {
    monthlyOption.value = emptyOption('當月每日收入 vs 去年同期', monthlyState.value.error || '無資料')
    return
  }
  monthlyOption.value = {
    title: { text: '當月每日收入 vs 去年同期' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['今年', '去年'] },
    xAxis: { type: 'category', data: monthlyData.value.dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '今年', type: 'line', smooth: true, data: monthlyData.value.revenues },
      { name: '去年', type: 'line', smooth: true, data: monthlyData.value.lastYear, lineStyle: { type: 'dashed' } },
    ],
  }
}

onMounted(async () => {
  // 用 allSettled：任一 API 失敗都不會影響其他區塊
  await Promise.allSettled([loadDaily(), loadRanking(), loadMonthly()])
  buildHourly(); buildRanking(); buildMonthly()
})

async function onDateChange() {
  await loadDaily()
  buildHourly()
}

async function retryRanking() {
  await loadRanking()
  buildRanking()
}

async function retryMonthly() {
  await loadMonthly()
  buildMonthly()
}
</script>

<template>
  <div>
    <h2 style="margin:0 0 20px">營收報表</h2>

    <!-- 日收入 -->
    <el-card style="margin-bottom:20px" v-loading="dailyState.loading">
      <div style="display:flex; align-items:center; gap:16px; margin-bottom:16px">
        <el-date-picker v-model="dailyDate" type="date" value-format="YYYY-MM-DD" @change="onDateChange" />
        <el-statistic v-if="dailyData" title="總收入" :value="dailyData.totalRevenue || 0" prefix="NT$" :precision="0" />
        <el-statistic v-if="dailyData" title="桌數" :value="dailyData.orderCount || 0" :precision="0" />
        <el-statistic v-if="dailyData" title="人數" :value="dailyData.guestCount || 0" :precision="0" />
        <el-alert v-if="dailyState.error" :title="`日報表載入失敗：${dailyState.error}`" type="error"
                  :closable="false" show-icon style="margin-left:auto; width:auto" />
      </div>
      <VChart style="height:300px" :option="hourlyOption" autoresize />
    </el-card>

    <el-row :gutter="20">
      <!-- 銷售排行 -->
      <el-col :span="12">
        <el-card v-loading="rankingState.loading">
          <template v-if="rankingState.error" #header>
            <div style="display:flex; justify-content:space-between; align-items:center">
              <span style="color:#f56c6c">銷售排行載入失敗</span>
              <el-button size="small" @click="retryRanking">重試</el-button>
            </div>
          </template>
          <VChart style="height:350px" :option="rankingOption" autoresize />
        </el-card>
      </el-col>

      <!-- 月趨勢 -->
      <el-col :span="12">
        <el-card v-loading="monthlyState.loading">
          <template v-if="monthlyState.error" #header>
            <div style="display:flex; justify-content:space-between; align-items:center">
              <span style="color:#f56c6c">月趨勢載入失敗</span>
              <el-button size="small" @click="retryMonthly">重試</el-button>
            </div>
          </template>
          <VChart style="height:350px" :option="monthlyOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
