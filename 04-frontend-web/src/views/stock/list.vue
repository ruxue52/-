<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">库存查询</h3>
        <div class="kpi-inline">
          <span>SKU：<b>{{ summary.totalSku ?? '-' }}</b></span>
          <span>总数量：<b>{{ summary.totalQty ?? '-' }}</b></span>
          <span>库存金额：<b class="text-danger">{{ formatMoney(summary.totalAmount) }}</b></span>
          <span>预警数：<b class="text-danger">{{ summary.warningCount ?? '-' }}</b></span>
        </div>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="书名/ISBN" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.onlyLow">只看低库存</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="quantity" label="在库数量" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'text-danger': Number(row.quantity) < Number(row.safetyStock || 0) }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="110" align="right" />
        <el-table-column prop="avgCost" label="平均成本" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.avgCost) }}</template>
        </el-table-column>
        <el-table-column prop="stockAmount" label="库存金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.stockAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="Number(row.quantity) < Number(row.safetyStock || 0)" type="danger" size="small">低库存</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无库存数据" /></template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { categoryApi, stockApi, warehouseApi } from '@/api'
import { formatMoney } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const warehouses = ref([])
const categories = ref([])
const summary = ref({})

const query = reactive({ keyword: '', warehouseId: null, categoryId: null, onlyLow: false, page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await stockApi.list({ ...query })
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    summary.value = (await stockApi.summary()) || {}
  } catch (e) {
    summary.value = {}
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.warehouseId = null
  query.categoryId = null
  query.onlyLow = false
  search()
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
    const [w, c] = await Promise.all([warehouseApi.list({ page: 1, size: 200 }), categoryApi.list({})])
    warehouses.value = (w && w.records) || []
    categories.value = flat(Array.isArray(c) ? c : (c && c.records) || [])
  } catch (e) {
    /* ignore */
  }
}

onMounted(() => {
  load()
  loadOptions()
  loadSummary()
})
</script>

<style scoped>
.kpi-inline {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #606266;
}
.kpi-inline b {
  font-size: 15px;
}
</style>