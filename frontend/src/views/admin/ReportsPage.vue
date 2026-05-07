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

async function loadDaily() {
  const res = await api.get(`/api/reports/daily?date=${dailyDate.value}`)
  dailyData.value = res.data.data
}

async function loadRanking() {
  const res = await api.get('/api/reports/ranking?limit=10')
  rankingData.value = res.data.data?.products || []
}

async function loadMonthly() {
  const res = await api.get('/api/reports/monthly')
  monthlyData.value = res.data.data
}

const hourlyOption = ref({})
const rankingOption = ref({})
const monthlyOption = ref({})

function buildHourly() {
  if (!dailyData.value) return
  hourlyOption.value = {
    title: { text: '今日每小時收入' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dailyData.value.hourly.map(h => `${h.hour}時`) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: dailyData.value.hourly.map(h => h.revenue), name: '收入' }],
  }
}

function buildRanking() {
  if (!rankingData.value.length) return
  rankingOption.value = {
    title: { text: '銷售排行 Top10' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: rankingData.value.map(r => r.productName).reverse() },
    series: [{ type: 'bar', data: rankingData.value.map(r => r.totalQuantity).reverse(), name: '銷售量' }],
  }
}

function buildMonthly() {
  if (!monthlyData.value) return
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
  await Promise.all([loadDaily(), loadRanking(), loadMonthly()])
  buildHourly(); buildRanking(); buildMonthly()
})

async function onDateChange() {
  await loadDaily()
  buildHourly()
}
</script>

<template>
  <div>
    <h2>營收報表</h2>

    <!-- 日收入 -->
    <el-card style="margin-bottom:20px">
      <div style="display:flex; align-items:center; gap:16px; margin-bottom:16px">
        <el-date-picker v-model="dailyDate" type="date" value-format="YYYY-MM-DD" @change="onDateChange" />
        <el-statistic v-if="dailyData" title="總收入" :value="dailyData.totalRevenue" prefix="NT$" :precision="0" />
        <el-statistic v-if="dailyData" title="桌數" :value="dailyData.orderCount" :precision="0" />
        <el-statistic v-if="dailyData" title="人數" :value="dailyData.guestCount" :precision="0" />
      </div>
      <VChart style="height:300px" :option="hourlyOption" autoresize />
    </el-card>

    <el-row :gutter="20">
      <!-- 銷售排行 -->
      <el-col :span="12">
        <el-card>
          <VChart style="height:350px" :option="rankingOption" autoresize />
        </el-card>
      </el-col>
      <!-- 月趨勢 -->
      <el-col :span="12">
        <el-card>
          <VChart style="height:350px" :option="monthlyOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
