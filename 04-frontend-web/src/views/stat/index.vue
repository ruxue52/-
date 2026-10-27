<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">查询统计</h3>
        <div class="header-actions">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="default"
            style="width: 260px"
            @change="reloadAll"
          />
          <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
        </div>
      </div>

      <el-tabs v-model="activeGroup">
        <el-tab-pane v-for="g in groups" :key="g.key" :label="g.label" :name="g.key">
          <el-row :gutter="16">
            <el-col v-for="item in groupItems(g.key)" :key="item.key" :xs="24" :lg="item.full ? 24 : 12" class="mb-16">
              <div class="chart-card">
                <div class="chart-title">{{ item.title }}</div>
                <div v-if="item.type === 'table'">
                  <el-table
                    v-loading="loadingMap[item.key]"
                    :data="tableMap[item.key]?.rows || []"
                    border
                    size="small"
                    style="width: 100%"
                    max-height="320"
                  >
                    <el-table-column
                      v-for="col in (tableMap[item.key]?.columns || [])"
                      :key="col"
                      :prop="col"
                      :label="col"
                      align="center"
                      show-overflow-tooltip
                    />
                    <template #empty><el-empty description="暂无数据" :image-size="60" /></template>
                  </el-table>
                </div>
                <ChartBox v-else v-loading="loadingMap[item.key]" :option="optionMap[item.key]" :height="320" />
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import ChartBox from '@/components/ChartBox.vue'
import { statApi } from '@/api'

const activeGroup = ref('sales')
const dateRange = ref([])

const groups = [
  { key: 'sales', label: '销售类' },
  { key: 'purchase', label: '采购类' },
  { key: 'stock', label: '库存类' },
  { key: 'profit', label: '毛利类' },
  { key: 'customer', label: '会员类' },
  { key: 'check', label: '盘点类' }
]

const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#8e44ad', '#00bcd4', '#ff9800', '#795548', '#3f51b5']

const optionMap = reactive({})
const tableMap = reactive({})
const loadingMap = reactive({})

function emptyOption() {
  return {
    title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#c0c4cc', fontSize: 14, fontWeight: 'normal' } },
    xAxis: { show: false },
    yAxis: { show: false },
    series: []
  }
}

function pieOption(list) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', bottom: 0 },
    color: palette,
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        data: list.map((i) => ({ name: i.name, value: Number(i.value || 0) })),
        label: { formatter: '{b}\n{d}%' }
      }
    ]
  }
}

function barOption(list, labelKey, valueKey, seriesName, isMoney) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 70, right: 30, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: list.map((i) => i[labelKey]),
      axisLabel: { formatter: (v) => (v && String(v).length > 8 ? String(v).slice(0, 8) + '…' : v) }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: seriesName,
        type: 'bar',
        barWidth: '48%',
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        data: list.map((i) => Number(i[valueKey] || 0)),
        label: isMoney ? { show: false } : undefined
      }
    ]
  }
}

function horizontalBarOption(list, labelKey, valueKey, seriesName) {
  const arr = [...list].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 130, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: arr.map((i) => i[labelKey]),
      axisLabel: { formatter: (v) => (v && String(v).length > 10 ? String(v).slice(0, 10) + '…' : v) }
    },
    series: [
      {
        name: seriesName,
        type: 'bar',
        barWidth: '55%',
        itemStyle: { color: '#e6a23c', borderRadius: [0, 4, 4, 0] },
        data: arr.map((i) => Number(i[valueKey] || 0))
      }
    ]
  }
}

function lineOption(list, xKey, series) {
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: series.map((s) => s.name), right: 10 },
    grid: { left: 60, right: 40, top: 40, bottom: 40 },
    xAxis: { type: 'category', boundaryGap: false, data: list.map((i) => i[xKey]) },
    yAxis: series.length > 1 ? [{ type: 'value' }, { type: 'value' }] : { type: 'value' },
    series: series.map((s, idx) => ({
      name: s.name,
      type: 'line',
      smooth: true,
      yAxisIndex: idx > 0 && series.length > 1 ? 1 : 0,
      areaStyle: idx === 0 ? { opacity: 0.12 } : undefined,
      itemStyle: { color: s.color },
      data: list.map((i) => Number(i[s.key] || 0))
    }))
  }
}

/** 通用表格归一化：数组取首行 key 作为列；对象转 KV 行 */
function normalizeTable(data) {
  if (Array.isArray(data)) {
    if (!data.length) return { columns: [], rows: [] }
    const columns = Object.keys(data[0])
    return { columns, rows: data }
  }
  if (data && typeof data === 'object') {
    return {
      columns: ['指标', '数值'],
      rows: Object.entries(data).map(([k, v]) => ({ 指标: k, 数值: v }))
    }
  }
  return { columns: [], rows: [] }
}

function dateParams() {
  const p = {}
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2 && dateRange.value[0]) {
    p.startDate = dateRange.value[0]
    p.endDate = dateRange.value[1]
  }
  return p
}

const items = [
  // 销售类
  { key: 'salesTrend', group: 'sales', title: '销售趋势（近30天）', type: 'chart',
    api: () => statApi.salesTrend({ days: 30 }),
    toOption: (l) => (!l.length ? emptyOption() : lineOption(l, 'date', [
      { name: '销售额', key: 'amount', color: '#409eff' },
      { name: '销量', key: 'qty', color: '#67c23a' }
    ])) },
  { key: 'salesMonthly', group: 'sales', title: '月度销售额', type: 'chart',
    api: () => statApi.salesMonthly({ year: new Date().getFullYear() }),
    toOption: (l) => (!l.length ? emptyOption() : barOption(l, 'month', 'amount', '销售额', true)) },
  { key: 'salesTopBooks', group: 'sales', title: '畅销书 TOP10', type: 'chart', full: true,
    api: () => statApi.salesTopBooks({ limit: 10, ...dateParams() }),
    toOption: (l) => (!l.length ? emptyOption() : horizontalBarOption(l.map((i) => ({ ...i, label: i.name || i.bookName || i.isbn })), 'label', 'qty', '销量')) },
  { key: 'salesByCategory', group: 'sales', title: '分类销售占比', type: 'chart',
    api: () => statApi.salesByCategory(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : pieOption(l)) },
  { key: 'salesByPublisher', group: 'sales', title: '出版社销售占比', type: 'chart',
    api: () => statApi.salesByPublisher(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : pieOption(l)) },
  { key: 'salesByWarehouse', group: 'sales', title: '各仓销售对比', type: 'chart',
    api: () => statApi.salesByWarehouse(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : barOption(l, 'name', 'value', '销售额', true)) },
  { key: 'salesByPayment', group: 'sales', title: '支付方式分布', type: 'chart',
    api: () => statApi.salesByPayment(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : pieOption(l)) },
  { key: 'salesByOperator', group: 'sales', title: '收银员业绩', type: 'chart',
    api: () => statApi.salesByOperator(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : horizontalBarOption(l.map((i) => ({ ...i, label: i.name || i.operatorName })), 'label', 'value', '销售额')) },
  { key: 'salesHour', group: 'sales', title: '时段销售分布', type: 'chart', full: true,
    api: () => statApi.salesHour(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : barOption(l.map((i) => ({ ...i, label: i.hour !== undefined ? i.hour + '时' : i.name })), 'label', 'amount', '销售额', true)) },

  // 采购类
  { key: 'purchaseTrend', group: 'purchase', title: '采购趋势（近30天）', type: 'chart', full: true,
    api: () => statApi.purchaseTrend({ days: 30 }),
    toOption: (l) => (!l.length ? emptyOption() : lineOption(l, 'date', [
      { name: '采购额', key: 'amount', color: '#e6a23c' },
      { name: '采购量', key: 'qty', color: '#409eff' }
    ])) },
  { key: 'purchaseBySupplier', group: 'purchase', title: '供应商采购汇总', type: 'chart',
    api: () => statApi.purchaseBySupplier(dateParams()),
    toOption: (l) => (!l.length ? emptyOption() : horizontalBarOption(l.map((i) => ({ ...i, label: i.name || i.supplierName })), 'label', 'value', '采购额')) },
  { key: 'purchaseTopBooks', group: 'purchase', title: '采购量最大图书 TOP10', type: 'chart',
    api: () => statApi.purchaseTopBooks({ limit: 10 }),
    toOption: (l) => (!l.length ? emptyOption() : horizontalBarOption(l.map((i) => ({ ...i, label: i.name || i.bookName || i.isbn })), 'label', 'qty', '采购量')) },

  // 库存类
  { key: 'stockValueByCategory', group: 'stock', title: '库存金额按分类', type: 'chart',
    api: () => statApi.stockValueByCategory(),
    toOption: (l) => (!l.length ? emptyOption() : pieOption(l)) },
  { key: 'stockTurnover', group: 'stock', title: '库存周转率', type: 'table', full: true,
    api: () => statApi.stockTurnover(dateParams()) },
  { key: 'stockAge', group: 'stock', title: '库龄分析', type: 'chart',
    api: () => statApi.stockAge(),
    toOption: (l) => (!l.length ? emptyOption() : barOption(l.map((i) => ({ ...i, label: i.range })), 'label', 'skuCount', 'SKU 数')) },

  // 毛利类
  { key: 'profitByBook', group: 'profit', title: '单品毛利排行 TOP10', type: 'chart',
    api: () => statApi.profitByBook({ limit: 10, ...dateParams() }),
    toOption: (l) => (!l.length ? emptyOption() : horizontalBarOption(l.map((i) => ({ ...i, label: i.name || i.bookName || i.isbn })), 'label', 'profit', '毛利')) },
  { key: 'profitMonthly', group: 'profit', title: '月度毛利', type: 'chart',
    api: () => statApi.profitMonthly({ year: new Date().getFullYear() }),
    toOption: (l) => (!l.length ? emptyOption() : barOption(l, 'month', 'profit', '毛利', true)) },

  // 会员类
  { key: 'customerConsumeRank', group: 'customer', title: '客户消费排行 TOP10', type: 'chart',
    api: () => statApi.customerConsumeRank({ limit: 10 }),
    toOption: (l) => (!l.length ? emptyOption() : horizontalBarOption(l.map((i) => ({ ...i, label: i.name || i.customerName })), 'label', 'amount', '消费额')) },
  { key: 'customerActive', group: 'customer', title: '活跃会员 / 复购率', type: 'table',
    api: () => statApi.customerActive({ days: 30 }) },
  { key: 'memberLevelDistribution', group: 'customer', title: '会员等级分布', type: 'chart',
    api: () => statApi.memberLevelDistribution(),
    toOption: (l) => (!l.length ? emptyOption() : pieOption(l.map((i) => ({ name: i.name || i.levelName, value: i.value !== undefined ? i.value : i.count })))) },

  // 盘点类
  { key: 'checkDiffSummary', group: 'check', title: '盘点差异汇总', type: 'table', full: true,
    api: () => statApi.checkDiffSummary(dateParams()) }
]

function groupItems(groupKey) {
  return items.filter((i) => i.group === groupKey)
}

function resolveItem(item) {
  loadingMap[item.key] = true
  item
    .api()
    .then((data) => {
      const list = Array.isArray(data) ? data : data ? [data] : []
      if (item.type === 'table') {
        const norm = normalizeTable(data)
        tableMap[item.key] = norm
      } else {
        optionMap[item.key] = item.toOption ? item.toOption(list) : emptyOption()
      }
    })
    .catch(() => {
      if (item.type === 'table') tableMap[item.key] = { columns: [], rows: [] }
      else optionMap[item.key] = emptyOption()
    })
    .finally(() => {
      loadingMap[item.key] = false
    })
}

function reloadAll() {
  items.forEach(resolveItem)
}

function initMaps() {
  items.forEach((item) => {
    if (item.type === 'table') tableMap[item.key] = { columns: [], rows: [] }
    else optionMap[item.key] = emptyOption()
    loadingMap[item.key] = false
  })
}

onMounted(() => {
  initMaps()
  reloadAll()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>