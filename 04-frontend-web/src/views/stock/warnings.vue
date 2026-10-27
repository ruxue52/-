<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">
          库存预警
          <el-tag type="danger" class="ml-8">{{ list.length }} 项低库存</el-tag>
        </h3>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="仓库">
          <el-select v-model="warehouseId" placeholder="全部仓库" clearable style="width: 200px" @change="load">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="quantity" label="当前库存" width="120" align="right">
          <template #default="{ row }"><span class="text-danger">{{ row.quantity }}</span></template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="120" align="right" />
        <el-table-column label="缺口" width="120" align="right">
          <template #default="{ row }">
            <span class="text-danger">{{ Math.max(0, Number(row.safetyStock || 0) - Number(row.quantity || 0)) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="shelfNo" label="货架位" width="120" />
        <template #empty><el-empty description="暂无低库存预警" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { stockApi, warehouseApi } from '@/api'

const loading = ref(false)
const list = ref([])
const warehouses = ref([])
const warehouseId = ref(null)

async function load() {
  loading.value = true
  try {
    const params = warehouseId.value ? { warehouseId: warehouseId.value } : {}
    const data = await stockApi.warnings(params)
    list.value = Array.isArray(data) ? data : (data && data.records) || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
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

<style scoped>
.ml-8 {
  margin-left: 8px;
}
</style>