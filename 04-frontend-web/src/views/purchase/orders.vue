<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">进货单管理</h3>
        <el-button type="primary" :icon="Plus" @click="$router.push('/purchase/create')">进货开单</el-button>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="单号/供应商" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="query.supplierId" placeholder="全部" clearable filterable style="width: 180px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="制单日期">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width: 250px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="orderNo" label="进货单号" width="170" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="入库仓库" width="120" />
        <el-table-column prop="totalQty" label="数量" width="90" align="right" />
        <el-table-column prop="totalAmount" label="总金额" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payAmount" label="应付金额" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="settleType" label="结算方式" width="100" align="center">
          <template #default="{ row }">{{ SETTLE_TYPE[row.settleType] || row.settleType }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(ORDER_STATUS, row.status)" size="small">
              {{ statusLabel(ORDER_STATUS, row.status) }}
            </el-tag>
            <el-tag v-if="row.isLocked === 1" type="success" size="small" class="lock-tag">已锁定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="经办人" width="110" />
        <el-table-column prop="createTime" label="制单时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/purchase/detail/${row.id}`)">详情</el-button>
            <el-button link type="info" size="small" @click="openDoc(row)">确认/修订</el-button>
            <el-button v-if="row.status === 'DRAFT' && row.isLocked !== 1" link type="warning" size="small" @click="act(row, 'submit', '提交')">提交</el-button>
            <el-button v-if="row.status === 'SUBMITTED' && row.isLocked !== 1" link type="success" size="small" @click="act(row, 'instock', '入库')">入库</el-button>
            <el-button
              v-if="row.status !== 'CANCELLED' && row.status !== 'INSTOCK' && row.isLocked !== 1"
              link
              type="danger"
              size="small"
              @click="act(row, 'cancel', '作废')"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无数据" /></template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <OrderDocDialog
      v-model="docVisible"
      order-type="IN"
      :order-id="docRow.id"
      :order-no="docRow.orderNo"
      :status="docRow.status"
      :is-locked="docRow.isLocked"
      :confirm-user-name="docRow.confirmUserName"
      :confirm-time="docRow.confirmTime"
      @changed="load"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { purchaseApi, supplierApi, warehouseApi } from '@/api'
import OrderDocDialog from '@/components/OrderDocDialog.vue'
import { ORDER_STATUS, SETTLE_TYPE, formatDate, formatMoney, statusLabel, statusType } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const suppliers = ref([])
const warehouses = ref([])
const dateRange = ref([])

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'INSTOCK', label: '已入库' },
  { value: 'CANCELLED', label: '已作废' }
]

const query = reactive({ keyword: '', status: '', supplierId: null, warehouseId: null, page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const data = await purchaseApi.list(params)
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.status = ''
  query.supplierId = null
  query.warehouseId = null
  dateRange.value = []
  search()
}

function act(row, action, label) {
  ElMessageBox.confirm(`确认${label}进货单「${row.orderNo}」吗？`, '操作确认', { type: 'warning' })
    .then(async () => {
      await purchaseApi[action](row.id)
      ElMessage.success(`${label}成功`)
      load()
    })
    .catch(() => {})
}

/* ---- 确认锁定 / 修订 ---- */
const docVisible = ref(false)
const docRow = ref({})

function openDoc(row) {
  docRow.value = row
  docVisible.value = true
}

async function loadOptions() {
  try {
    const [s, w] = await Promise.all([supplierApi.list({ page: 1, size: 200 }), warehouseApi.list({ page: 1, size: 200 })])
    suppliers.value = (s && s.records) || []
    warehouses.value = (w && w.records) || []
  } catch (e) {
    /* ignore */
  }
}

onMounted(() => {
  load()
  loadOptions()
})
</script>

<style scoped>
.lock-tag {
  margin-left: 4px;
}
</style>