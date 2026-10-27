<template>
  <div class="page-container" v-loading="loading">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">
          盘点单 {{ order.checkNo || '' }}
          <el-tag v-if="order.status" :type="statusType(ORDER_STATUS, order.status)" class="ml-8">
            {{ statusLabel(ORDER_STATUS, order.status) }}
          </el-tag>
        </h3>
        <div>
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary" :disabled="readonly" :loading="saving" @click="saveItems">保存实盘数</el-button>
          <el-button type="success" :disabled="readonly" @click="finish">完成盘点</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border class="mb-16">
        <el-descriptions-item label="盘点仓库">{{ order.warehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="盘点类型">{{ CHECK_TYPE[order.checkType] || order.checkType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="盘点人">{{ order.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品种数">{{ order.totalItems ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="差异品种">{{ order.diffItems ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="盘盈/盘亏">
          <span class="text-success">{{ order.profitQty ?? 0 }}</span> /
          <span class="text-danger">{{ order.lossQty ?? 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(order.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(order.finishTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="toolbar">
        <el-button size="small" @click="fillAllReal">全部按账面填充</el-button>
        <el-button size="small" @click="fillZero">全部清零</el-button>
        <span class="diff-tip">差异品种：<b class="text-danger">{{ diffCount }}</b></span>
      </div>

      <el-table :data="items" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bookQty" label="账面数量" width="110" align="right" />
        <el-table-column label="实盘数量" width="150">
          <template #default="{ row }">
            <el-input-number
              v-model="row.realQty"
              :min="0"
              :disabled="readonly"
              size="small"
              controls-position="right"
              style="width: 120px"
            />
          </template>
        </el-table-column>
        <el-table-column label="差异数量" width="110" align="right">
          <template #default="{ row }">
            <span :class="diffOf(row) > 0 ? 'text-success' : diffOf(row) < 0 ? 'text-danger' : ''">
              {{ diffOf(row) > 0 ? '+' : '' }}{{ diffOf(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="diffAmount" label="差异金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.diffAmount) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <template #empty><el-empty description="无盘点明细" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stockApi } from '@/api'
import { CHECK_TYPE, ORDER_STATUS, formatDate, formatMoney, statusLabel, statusType } from '@/utils/dict'

const route = useRoute()
const id = route.params.id
const loading = ref(false)
const saving = ref(false)
const order = ref({})
const items = ref([])

const readonly = computed(() => order.value.status !== 'DRAFT')

function diffOf(row) {
  return Number(row.realQty || 0) - Number(row.bookQty || 0)
}

const diffCount = computed(() => items.value.filter((i) => diffOf(i) !== 0).length)

async function load() {
  loading.value = true
  try {
    const data = (await stockApi.checkDetail(id)) || {}
    order.value = data
    items.value = (data.items || []).map((i) => ({ ...i, realQty: Number(i.realQty ?? i.bookQty ?? 0) }))
  } catch (e) {
    order.value = {}
    items.value = []
  } finally {
    loading.value = false
  }
}

function fillAllReal() {
  items.value.forEach((i) => {
    i.realQty = Number(i.bookQty || 0)
  })
}

function fillZero() {
  items.value.forEach((i) => {
    i.realQty = 0
  })
}

async function saveItems() {
  saving.value = true
  try {
    await stockApi.checkSaveItems(id, {
      items: items.value.map((i) => ({ itemId: i.id, realQty: Number(i.realQty) }))
    })
    ElMessage.success('实盘数已保存')
    load()
  } catch (e) {
    /* ignore */
  } finally {
    saving.value = false
  }
}

function finish() {
  ElMessageBox.confirm('确认完成盘点？完成后将按差异调整库存。', '完成盘点', { type: 'warning' })
    .then(async () => {
      await stockApi.checkFinish(id)
      ElMessage.success('盘点已完成')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.ml-8 {
  margin-left: 8px;
}
.diff-tip {
  margin-left: 12px;
  font-size: 13px;
  line-height: 32px;
}
</style>