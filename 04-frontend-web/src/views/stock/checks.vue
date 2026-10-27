<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">库存盘点</h3>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建盘点单</el-button>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="checkNo" label="盘点单号" width="170" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="checkType" label="盘点类型" width="100" align="center">
          <template #default="{ row }">{{ CHECK_TYPE[row.checkType] || row.checkType }}</template>
        </el-table-column>
        <el-table-column prop="totalItems" label="品种数" width="90" align="right" />
        <el-table-column prop="diffItems" label="差异品种" width="100" align="right" />
        <el-table-column prop="profitQty" label="盘盈" width="90" align="right" class-name="text-success" />
        <el-table-column prop="lossQty" label="盘亏" width="90" align="right" class-name="text-danger" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(ORDER_STATUS, row.status)" size="small">
              {{ statusLabel(ORDER_STATUS, row.status) }}
            </el-tag>
            <el-tag v-if="row.isLocked === 1" type="success" size="small" class="lock-tag">已锁定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="盘点人" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/stock/check/${row.id}`)">录入/详情</el-button>
            <el-button link type="info" size="small" @click="openDoc(row)">确认/修订</el-button>
            <el-button v-if="row.status === 'DRAFT' && row.isLocked !== 1" link type="success" size="small" @click="finish(row)">完成</el-button>
            <el-button v-if="row.status === 'DRAFT' && row.isLocked !== 1" link type="danger" size="small" @click="cancel(row)">作废</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无盘点单" /></template>
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
      order-type="CHECK"
      :order-id="docRow.id"
      :order-no="docRow.checkNo"
      :status="docRow.status"
      :is-locked="docRow.isLocked"
      :confirm-user-name="docRow.confirmUserName"
      :confirm-time="docRow.confirmTime"
      @changed="load"
    />

    <el-dialog v-model="dialogVisible" title="新建盘点单" width="640px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="盘点仓库" required>
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型">
          <el-radio-group v-model="form.checkType">
            <el-radio-button value="FULL">全盘</el-radio-button>
            <el-radio-button value="PART">抽盘</el-radio-button>
            <el-radio-button value="CYCLE">循环盘</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="指定图书">
          <el-select
            v-model="form.bookIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="不选则为全盘"
            :remote-method="searchBooks"
            :loading="bookLoading"
            style="width: 100%"
          >
            <el-option v-for="b in bookOptions" :key="b.id" :label="`${b.bookName}（${b.isbn}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { bookApi, stockApi, warehouseApi } from '@/api'
import OrderDocDialog from '@/components/OrderDocDialog.vue'
import { CHECK_TYPE, ORDER_STATUS, formatDate, statusLabel, statusType } from '@/utils/dict'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const warehouses = ref([])

const statusOptions = [
  { value: 'DRAFT', label: '盘点中' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'CANCELLED', label: '已作废' }
]

const query = reactive({ warehouseId: null, status: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await stockApi.checkList({ ...query })
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
  query.warehouseId = null
  query.status = ''
  search()
}

const dialogVisible = ref(false)
const form = reactive({ warehouseId: null, checkType: 'FULL', remark: '', bookIds: [] })
const bookOptions = ref([])
const bookLoading = ref(false)

function openCreate() {
  form.warehouseId = null
  form.checkType = 'FULL'
  form.remark = ''
  form.bookIds = []
  dialogVisible.value = true
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

async function save() {
  if (!form.warehouseId) return ElMessage.warning('请选择盘点仓库')
  saving.value = true
  try {
    const data = await stockApi.checkCreate({
      warehouseId: form.warehouseId,
      checkType: form.checkType,
      remark: form.remark,
      bookIds: form.bookIds
    })
    ElMessage.success('盘点单已创建')
    dialogVisible.value = false
    const id = data && (data.id || data.orderId)
    if (id) router.push(`/stock/check/${id}`)
    else load()
  } catch (e) {
    /* ignore */
  } finally {
    saving.value = false
  }
}

function finish(row) {
  ElMessageBox.confirm(`确认完成盘点单「${row.checkNo}」？将按差异调整库存。`, '操作确认', { type: 'warning' })
    .then(async () => {
      await stockApi.checkFinish(row.id)
      ElMessage.success('盘点已完成')
      load()
    })
    .catch(() => {})
}

function cancel(row) {
  ElMessageBox.confirm(`确认作废盘点单「${row.checkNo}」吗？`, '操作确认', { type: 'warning' })
    .then(async () => {
      await stockApi.checkCancel(row.id)
      ElMessage.success('已作废')
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
.lock-tag {
  margin-left: 4px;
}
</style>