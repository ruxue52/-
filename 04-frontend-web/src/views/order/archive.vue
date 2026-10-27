<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">单据归档</h3>
        <div class="head-actions">
          <span class="label">保留天数</span>
          <el-input-number v-model="keepDays" :min="0" :max="3650" />
          <el-button :icon="Refresh" @click="loadSummary">刷新统计</el-button>
        </div>
      </div>

      <el-alert
        class="tip"
        type="info"
        :closable="false"
        show-icon
        title="仅「已执行 / 已作废」且「已确认锁定」的单据可归档；归档为逻辑标记，不删除数据，归档后常规列表不再显示，仅在本页可查。"
      />

      <el-table v-loading="sumLoading" :data="summary" border stripe style="width: 100%">
        <el-table-column prop="label" label="单据类型" width="130" />
        <el-table-column prop="candidates" label="待归档（早于保留期）" width="200" align="right" />
        <el-table-column prop="archivedTotal" label="已归档" width="120" align="right" />
        <el-table-column label="操作" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="!row.candidates || archiving === row.orderType"
              :loading="archiving === row.orderType"
              @click="batchArchive(row)"
            >
              批量归档
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">已归档单据</h3>
        <div class="head-actions">
          <el-select v-model="query.orderType" style="width: 130px" @change="reloadArchived">
            <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="单据号"
            clearable
            style="width: 200px"
            @keyup.enter="reloadArchived"
          />
          <el-button type="primary" :icon="Search" @click="reloadArchived">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="archived" border stripe style="width: 100%">
        <el-table-column label="单据号" width="200">
          <template #default="{ row }">{{ row[noKey] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(ORDER_STATUS, row.status)" size="small">
              {{ statusLabel(ORDER_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="archiveUserName" label="归档人" width="120" />
        <el-table-column prop="archiveTime" label="归档时间" width="175">
          <template #default="{ row }">{{ formatDate(row.archiveTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDoc(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无已归档单据" /></template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadArchived"
          @current-change="loadArchived"
        />
      </div>
    </div>

    <OrderDocDialog
      v-model="docVisible"
      :order-type="query.orderType"
      :order-id="docRow.id"
      :order-no="docRow[noKey]"
      :status="docRow.status"
      :is-locked="docRow.isLocked"
      :is-archived="docRow.isArchived"
      :archive-user-name="docRow.archiveUserName"
      :archive-time="docRow.archiveTime"
      :confirm-user-name="docRow.confirmUserName"
      :confirm-time="docRow.confirmTime"
      @changed="refreshAll"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { orderDocApi } from '@/api'
import OrderDocDialog from '@/components/OrderDocDialog.vue'
import { ORDER_STATUS, formatDate, statusLabel, statusType } from '@/utils/dict'

const TYPE_OPTIONS = [
  { value: 'IN', label: '进货单' },
  { value: 'OUT', label: '出货单' },
  { value: 'CHECK', label: '盘点单' },
  { value: 'TRANSFER', label: '调拨单' },
  { value: 'RETURN', label: '退货单' }
]
const NO_KEY = { IN: 'orderNo', OUT: 'orderNo', CHECK: 'checkNo', TRANSFER: 'transferNo', RETURN: 'returnNo' }

const keepDays = ref(180)
const summary = ref([])
const sumLoading = ref(false)
const archiving = ref('')
const archived = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ orderType: 'IN', keyword: '', page: 1, size: 10 })
const noKey = computed(() => NO_KEY[query.orderType] || 'orderNo')

const docVisible = ref(false)
const docRow = ref({})

async function loadSummary() {
  sumLoading.value = true
  try {
    const data = await orderDocApi.archiveSummary({ keepDays: keepDays.value })
    summary.value = Array.isArray(data) ? data : []
  } catch (e) {
    summary.value = []
  } finally {
    sumLoading.value = false
  }
}

function loadArchived() {
  loading.value = true
  orderDocApi
    .archivedList(query.orderType, {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    })
    .then((data) => {
      archived.value = (data && data.records) || []
      total.value = (data && data.total) || 0
    })
    .catch(() => {
      archived.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function reloadArchived() {
  query.page = 1
  loadArchived()
}

function batchArchive(row) {
  ElMessageBox.confirm(
    `将归档 ${row.label} 中创建时间早于 ${keepDays.value} 天前的 ${row.candidates} 张单据，确认执行？`,
    '批量归档确认',
    { type: 'warning' }
  )
    .then(async () => {
      archiving.value = row.orderType
      const count = await orderDocApi.archiveBatch(row.orderType, keepDays.value)
      ElMessage.success(`已归档 ${count} 张单据`)
      refreshAll()
    })
    .catch(() => {})
    .finally(() => {
      archiving.value = ''
    })
}

function openDoc(row) {
  docRow.value = row
  docVisible.value = true
}

function refreshAll() {
  loadSummary()
  loadArchived()
}

onMounted(refreshAll)
</script>

<style scoped>
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  font-size: 13px;
  color: #606266;
}
.tip {
  margin-bottom: 12px;
}
</style>
