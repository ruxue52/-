<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">库存流水</h3>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="图书">
          <el-select
            v-model="query.bookId"
            filterable
            remote
            reserve-keyword
            placeholder="输入书名/ISBN 搜索"
            :remote-method="searchBooks"
            :loading="bookLoading"
            clearable
            style="width: 200px"
          >
            <el-option v-for="b in bookOptions" :key="b.id" :label="`${b.bookName}（${b.isbn}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="query.bizType" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="o in bizOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="bizType" label="业务类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.direction === 1 ? 'success' : 'danger'" size="small">
              {{ BIZ_TYPE[row.bizType] || row.bizType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" width="170" show-overflow-tooltip />
        <el-table-column prop="bookName" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column label="方向" width="80" align="center">
          <template #default="{ row }">
            <span :class="row.direction === 1 ? 'text-success' : 'text-danger'">
              {{ row.direction === 1 ? '入库' : '出库' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column prop="beforeQty" label="变动前" width="90" align="right" />
        <el-table-column prop="afterQty" label="变动后" width="90" align="right" />
        <el-table-column prop="unitCost" label="单位成本" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.unitCost) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="createTime" label="发生时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无流水记录" /></template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
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
import { bookApi, stockApi, warehouseApi } from '@/api'
import { BIZ_TYPE, formatDate, formatMoney, toOptions } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const warehouses = ref([])
const bookOptions = ref([])
const bookLoading = ref(false)
const dateRange = ref([])

const bizOptions = toOptions(BIZ_TYPE)

const query = reactive({ bookId: null, warehouseId: null, bizType: '', startDate: '', endDate: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  const params = { ...query }
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2 && dateRange.value[0]) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  } else {
    params.startDate = ''
    params.endDate = ''
  }
  try {
    const data = await stockApi.flows(params)
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
  query.bookId = null
  query.warehouseId = null
  query.bizType = ''
  dateRange.value = []
  search()
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

async function loadWarehouses() {
  try {
    const data = await warehouseApi.list({ page: 1, size: 200 })
    warehouses.value = (data && data.records) || []
  } catch (e) {
    /* ignore */
  }
}

onMounted(() => {
  load()
  loadWarehouses()
})
</script>