<template>
  <div class="page-container">
    <!-- KPI 卡片 -->
    <el-row :gutter="16">
      <el-col v-for="kpi in kpis" :key="kpi.key" :xs="12" :sm="8" :md="8" :lg="4" class="mb-16">
        <div class="kpi-card" :style="{ background: kpi.color }">
          <el-icon class="kpi-icon"><component :is="kpi.icon" /></el-icon>
          <div>
            <div class="kpi-value">{{ kpi.value }}</div>
            <div class="kpi-label">{{ kpi.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="16">
        <div class="chart-card">
          <div class="flex-between">
            <span class="chart-title">销售趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">近7天</el-radio-button>
              <el-radio-button :value="30">近30天</el-radio-button>
              <el-radio-button :value="90">近90天</el-radio-button>
            </el-radio-group>
          </div>
          <ChartBox :option="trendOption" :height="330" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="chart-card">
          <div class="chart-title">分类销售占比</div>
          <ChartBox :option="categoryOption" :height="330" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-title">畅销书 TOP10</div>
          <ChartBox :option="topBooksOption" :height="340" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-title">月度销售额</div>
          <ChartBox :option="monthlyOption" :height="340" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ChartBox from '@/components/ChartBox.vue'
import { statApi } from '@/api'
import { formatMoney } from '@/utils/dict'

const kpis = ref([
  { key: 'todayAmount', label: '今日销售额', value: '0.00', icon: 'Money', color: '#409eff' },
  { key: 'todayOrders', label: '今日单数', value: '0', icon: 'Tickets', color: '#67c23a' },
  { key: 'monthAmount', label: '本月销售额', value: '0.00', icon: 'TrendCharts', color: '#e6a23c' },
  { key: 'stockAmount', label: '库存总额', value: '0.00', icon: 'Box', color: '#909399' },
  { key: 'warningCount', label: '库存预警数', value: '0', icon: 'WarningFilled', color: '#f56c6c' },
  { key: 'pendingCount', label: '待处理单据', value: '0', icon: 'AlarmClock', color: '#8e44ad' }
])

const trendDays = ref(30)
const trendOption = ref({})
const categoryOption = ref({})
const topBooksOption = ref({})
const monthlyOption = ref({})

const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#8e44ad', '#00bcd4', '#ff9800', '#795548']

async function loadOverview() {
  try {
    const d = (await statApi.overview()) || {}
    const map = {
      todayAmount: formatMoney(d.todayAmount ?? d.todaySalesAmount),
      todayOrders: d.todayOrders ?? d.todayOrderCount ?? 0,
      monthAmount: formatMoney(d.monthAmount ?? d.monthSalesAmount),
      stockAmount: formatMoney(d.stockAmount ?? d.totalStockAmount),
      warningCount: d.warningCount ?? 0,
      pendingCount: d.pendingCount ?? d.pendingOrders ?? 0
    }
    kpis.value = kpis.value.map((k) => ({ ...k, value: map[k.key] }))
  } catch (e) {
    /* 拦截器已提示 */
  }
}

function emptyOption(title) {
  return {
    title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#c0c4cc', fontSize: 14, fontWeight: 'normal' } },
    xAxis: { show: false },
    yAxis: { show: false },
    series: []
  }
}

async function loadTrend() {
  try {
    const list = (await statApi.salesTrend({ days: trendDays.value })) || []
    if (!list.length) {
      trendOption.value = emptyOption()
      return
    }
    trendOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '销量'], right: 10 },
      grid: { left: 50, right: 30, top: 40, bottom: 40 },
      xAxis: { type: 'category', data: list.map((i) => i.date), boundaryGap: false },
      yAxis: [
        { type: 'value', name: '销售额' },
        { type: 'value', name: '销量' }
      ],
      series: [
        {
          name: '销售额',
          type: 'line',
          smooth: true,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#409eff' },
          data: list.map((i) => Number(i.amount || 0))
        },
        {
          name: '销量',
          type: 'line',
          smooth: true,
          yAxisIndex: 1,
          itemStyle: { color: '#67c23a' },
          data: list.map((i) => Number(i.qty || 0))
        }
      ]
    }
  } catch (e) {
    trendOption.value = emptyOption()
  }
}

async function loadCategory() {
  try {
    const list = (await statApi.salesByCategory({})) || []
    if (!list.length) {
      categoryOption.value = emptyOption()
      return
    }
    categoryOption.value = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { type: 'scroll', bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          data: list.map((i) => ({ name: i.name, value: Number(i.value || 0) })),
          label: { formatter: '{b}\n{d}%' }
        }
      ],
      color: palette
    }
  } catch (e) {
    categoryOption.value = emptyOption()
  }
}

async function loadTopBooks() {
  try {
    const list = (await statApi.salesTopBooks({ limit: 10 })) || []
    if (!list.length) {
      topBooksOption.value = emptyOption()
      return
    }
    const arr = [...list].reverse()
    topBooksOption.value = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 120, right: 40, top: 20, bottom: 30 },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: arr.map((i) => i.name || i.bookName || i.isbn),
        axisLabel: { formatter: (v) => (v && v.length > 8 ? v.slice(0, 8) + '…' : v) }
      },
      series: [
        {
          type: 'bar',
          barWidth: '55%',
          itemStyle: { color: '#e6a23c', borderRadius: [0, 4, 4, 0] },
          data: arr.map((i) => Number(i.qty || i.value || i.amount || 0))
        }
      ]
    }
  } catch (e) {
    topBooksOption.value = emptyOption()
  }
}

async function loadMonthly() {
  try {
    const year = new Date().getFullYear()
    const list = (await statApi.salesMonthly({ year })) || []
    if (!list.length) {
      monthlyOption.value = emptyOption()
      return
    }
    monthlyOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 60, right: 30, top: 30, bottom: 40 },
      xAxis: { type: 'category', data: list.map((i) => i.month) },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          name: '销售额',
          barWidth: '48%',
          itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
          data: list.map((i) => Number(i.amount || 0))
        }
      ]
    }
  } catch (e) {
    monthlyOption.value = emptyOption()
  }
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadCategory()
  loadTopBooks()
  loadMonthly()
})
</script>