<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">报表中心</h3>
        <el-button type="primary" :icon="Download" :disabled="!current" @click="exportExcel">导出 Excel</el-button>
      </div>

      <!-- 报表选择卡片（按岗位逐张授权，无权限的报表不展示） -->
      <div class="report-cards">
        <div
          v-for="r in visibleReports"
          :key="r.code"
          class="report-card"
          :class="{ active: current && current.code === r.code }"
          @click="selectReport(r)"
        >
          <div class="report-code">{{ r.code }}</div>
          <div class="report-name">{{ r.name }}</div>
        </div>
      </div>

      <!-- 查询参数 -->
      <el-form v-if="current" :inline="true" class="search-bar" @submit.prevent>
        <el-form-item v-for="f in current.params" :key="f.key" :label="f.label">
          <el-date-picker
            v-if="f.type === 'daterange'"
            v-model="params.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
          <el-date-picker
            v-else-if="f.type === 'month'"
            v-model="params.month"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            style="width: 160px"
          />
          <el-date-picker
            v-else-if="f.type === 'date'"
            v-model="params.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 160px"
          />
          <el-input
            v-else-if="f.type === 'checkNo'"
            v-model="params.checkNo"
            placeholder="盘点单号"
            clearable
            style="width: 180px"
          />
          <el-input-number
            v-else-if="f.type === 'limit'"
            v-model="params.limit"
            :min="1"
            :max="500"
            style="width: 130px"
          />
          <el-select
            v-else-if="f.type === 'supplier'"
            v-model="params.supplierId"
            placeholder="全部"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
          <el-select
            v-else-if="f.type === 'warehouse'"
            v-model="params.warehouseId"
            placeholder="全部"
            clearable
            style="width: 170px"
          >
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
          <el-select
            v-else-if="f.type === 'category'"
            v-model="params.categoryId"
            placeholder="全部"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
          <el-select
            v-else-if="f.type === 'level'"
            v-model="params.levelId"
            placeholder="全部"
            clearable
            style="width: 170px"
          >
            <el-option v-for="l in levels" :key="l.id" :label="l.levelName" :value="l.id" />
          </el-select>
          <el-select
            v-else-if="f.type === 'customer'"
            v-model="params.customerId"
            filterable
            remote
            reserve-keyword
            placeholder="输入客户名搜索"
            clearable
            :remote-method="searchCustomers"
            :loading="customerLoading"
            style="width: 200px"
          >
            <el-option v-for="c in customerOptions" :key="c.id" :label="`${c.customerName}（${c.phone || '-'}）`" :value="c.id" />
          </el-select>
          <el-select
            v-else-if="f.type === 'book'"
            v-model="params.bookId"
            filterable
            remote
            reserve-keyword
            placeholder="输入书名/ISBN"
            clearable
            :remote-method="searchBooks"
            :loading="bookLoading"
            style="width: 220px"
          >
            <el-option v-for="b in bookOptions" :key="b.id" :label="`${b.bookName}（${b.isbn}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 报表结果 -->
      <template v-if="current">
        <div class="report-title">{{ result.title || current.name }}</div>
        <el-table v-loading="loading" :data="result.rows" border stripe style="width: 100%" max-height="480">
          <el-table-column type="index" label="#" width="55" align="center" />
          <el-table-column
            v-for="col in result.columns"
            :key="col.key"
            :prop="col.key"
            :label="col.label"
            align="center"
            show-overflow-tooltip
            min-width="110"
          />
          <template #empty><el-empty description="暂无数据" /></template>
        </el-table>

        <div v-if="summaryRows.length" class="summary-bar">
          <span class="summary-title">汇总：</span>
          <span v-for="s in summaryRows" :key="s.key" class="summary-item">
            {{ s.label }}：<b>{{ s.value }}</b>
          </span>
        </div>
      </template>
      <el-empty v-else description="请选择上方报表" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import { bookApi, categoryApi, customerApi, memberLevelApi, reportApi, supplierApi, warehouseApi } from '@/api'
import { downloadFile } from '@/api/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const reports = [
  { code: 'R01', name: '销售日报表', params: [{ key: 'date', label: '日期', type: 'date' }] },
  { code: 'R02', name: '销售月报表', params: [{ key: 'month', label: '月份', type: 'month' }] },
  { code: 'R03', name: '商品销售排行榜', params: [{ key: 'dateRange', label: '日期', type: 'daterange' }, { key: 'limit', label: '数量', type: 'limit' }] },
  { code: 'R04', name: '分类销售汇总表', params: [{ key: 'dateRange', label: '日期', type: 'daterange' }] },
  { code: 'R05', name: '采购到货明细表', params: [{ key: 'dateRange', label: '日期', type: 'daterange' }, { key: 'supplierId', label: '供应商', type: 'supplier' }] },
  { code: 'R06', name: '供应商采购汇总表', params: [{ key: 'dateRange', label: '日期', type: 'daterange' }] },
  { code: 'R07', name: '库存结存表', params: [{ key: 'warehouseId', label: '仓库', type: 'warehouse' }, { key: 'categoryId', label: '分类', type: 'category' }] },
  { code: 'R08', name: '库存预警表', params: [{ key: 'warehouseId', label: '仓库', type: 'warehouse' }] },
  { code: 'R09', name: '库龄分析表', params: [{ key: 'warehouseId', label: '仓库', type: 'warehouse' }] },
  { code: 'R10', name: '盘点差异表', params: [{ key: 'checkNo', label: '盘点单号', type: 'checkNo' }, { key: 'dateRange', label: '日期', type: 'daterange' }] },
  { code: 'R11', name: '商品毛利分析表', params: [{ key: 'dateRange', label: '日期', type: 'daterange' }] },
  { code: 'R12', name: '客户消费明细表', params: [{ key: 'customerId', label: '客户', type: 'customer' }, { key: 'dateRange', label: '日期', type: 'daterange' }] },
  { code: 'R13', name: '会员积分报表', params: [{ key: 'levelId', label: '会员等级', type: 'level' }] },
  { code: 'R14', name: '库存流水台账', params: [{ key: 'bookId', label: '图书', type: 'book' }, { key: 'warehouseId', label: '仓库', type: 'warehouse' }, { key: 'dateRange', label: '日期', type: 'daterange' }] },
  { code: 'R15', name: '进销存汇总表', params: [{ key: 'month', label: '月份', type: 'month' }, { key: 'warehouseId', label: '仓库', type: 'warehouse' }] }
]

const current = ref(null)
const loading = ref(false)
const result = reactive({ title: '', columns: [], rows: [], summary: {} })

/**
 * 报表可见性用严格判据（要求精确持有 report:Rxx），不走 store 的 hasPerm：
 * hasPerm 有「空权限数组全放行」的兜底，那会让无权限用户看到全部 15 张卡片；
 * 且后端 PermInterceptor 也是按 report:<CODE> 精确校验，前端必须与之一致。
 */
const visibleReports = computed(() => {
  if (userStore.isAdmin) return reports
  const perms = userStore.permissions || []
  if (perms.includes('*')) return reports
  return reports.filter((r) => perms.includes(`report:${r.code}`))
})

const params = reactive({
  dateRange: [],
  month: '',
  date: '',
  checkNo: '',
  limit: 20,
  supplierId: null,
  warehouseId: null,
  categoryId: null,
  levelId: null,
  customerId: null,
  bookId: null
})

const suppliers = ref([])
const warehouses = ref([])
const categories = ref([])
const levels = ref([])
const customerOptions = ref([])
const customerLoading = ref(false)
const bookOptions = ref([])
const bookLoading = ref(false)

const summaryRows = computed(() => {
  const s = result.summary
  if (!s || typeof s !== 'object') return []
  return Object.entries(s).map(([key, value]) => ({ key, label: key, value }))
})

function buildParams() {
  const p = {}
  current.value.params.forEach((f) => {
    if (f.type === 'daterange') {
      if (Array.isArray(params.dateRange) && params.dateRange.length === 2 && params.dateRange[0]) {
        p.startDate = params.dateRange[0]
        p.endDate = params.dateRange[1]
      }
    } else if (f.key === 'date') {
      if (params.date) p.date = params.date
    } else if (f.key === 'month') {
      if (params.month) p.month = params.month
    } else if (f.key === 'checkNo') {
      if (params.checkNo) p.checkNo = params.checkNo
    } else if (f.type === 'limit') {
      if (params.limit) p.limit = params.limit
    } else {
      const v = params[f.key]
      if (v !== null && v !== undefined && v !== '') p[f.key] = v
    }
  })
  return p
}

async function load() {
  if (!current.value) return
  loading.value = true
  try {
    const data = await reportApi.get(current.value.code, buildParams())
    result.title = (data && data.title) || current.value.name
    result.columns = (data && data.columns) || []
    result.rows = (data && data.rows) || []
    result.summary = (data && data.summary) || {}
  } catch (e) {
    result.columns = []
    result.rows = []
    result.summary = {}
  } finally {
    loading.value = false
  }
}

function selectReport(r) {
  current.value = r
  params.dateRange = []
  params.month = ''
  params.date = ''
  params.checkNo = ''
  params.limit = 20
  params.supplierId = null
  params.warehouseId = null
  params.categoryId = null
  params.levelId = null
  params.customerId = null
  params.bookId = null
  result.title = ''
  result.columns = []
  result.rows = []
  result.summary = {}
  load()
}

function exportExcel() {
  if (!current.value) return
  downloadFile(`/report/${current.value.code}/export`, { format: 'xlsx', ...buildParams() })
    .then(() => ElMessage.success('导出中，请稍候'))
    .catch(() => {})
}

async function searchCustomers(kw) {
  if (!kw) return
  customerLoading.value = true
  try {
    const data = await customerApi.list({ keyword: kw, page: 1, size: 20 })
    customerOptions.value = (data && data.records) || []
  } catch (e) {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

async function searchBooks(kw) {
  if (!kw) return
  bookLoading.value = true
  try {
    const data = await bookApi.list({ keyword: kw, page: 1, size: 20 })
    bookOptions.value = (data && data.records) || []
  } catch (e) {
    bookOptions.value = []
  } finally {
    bookLoading.value = false
  }
}

function flat(tree, out = [], prefix = '') {
  ;(tree || []).forEach((n) => {
    out.push({ value: n.id, label: prefix + n.categoryName })
    if (n.children && n.children.length) flat(n.children, out, prefix + n.categoryName + ' / ')
  })
  return out
}

async function loadOptions() {
  try {
    const [s, w, c, l] = await Promise.all([
      supplierApi.list({ page: 1, size: 200 }),
      warehouseApi.list({ page: 1, size: 200 }),
      categoryApi.list({}),
      memberLevelApi.list({ page: 1, size: 100 })
    ])
    suppliers.value = (s && s.records) || []
    warehouses.value = (w && w.records) || []
    categories.value = flat(Array.isArray(c) ? c : (c && c.records) || [])
    levels.value = (l && l.records) || (Array.isArray(l) ? l : [])
  } catch (e) {
    /* ignore */
  }
}

onMounted(loadOptions)
</script>

<style scoped>
.report-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.report-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.report-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.15);
}
.report-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.report-code {
  font-size: 13px;
  font-weight: 700;
  color: #409eff;
}
.report-name {
  margin-top: 6px;
  font-size: 14px;
  color: #303133;
}
.report-title {
  font-size: 15px;
  font-weight: 600;
  margin: 10px 0;
  color: #303133;
}
.summary-bar {
  margin-top: 12px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
.summary-title {
  font-weight: 600;
  color: #303133;
}
.summary-item b {
  color: #409eff;
}
</style>