<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">库存调拨</h3>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建调拨单</el-button>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="调出仓库">
          <el-select v-model="query.fromWarehouse" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调入仓库">
          <el-select v-model="query.toWarehouse" placeholder="全部" clearable style="width: 170px">
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
        <el-table-column prop="transferNo" label="调拨单号" width="180" />
        <el-table-column label="调出仓库" width="140">
          <template #default="{ row }">{{ warehouseName(row.fromWarehouse) }}</template>
        </el-table-column>
        <el-table-column label="调入仓库" width="140">
          <template #default="{ row }">{{ warehouseName(row.toWarehouse) }}</template>
        </el-table-column>
        <el-table-column prop="totalQty" label="调拨数量" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(ORDER_STATUS, row.status)" size="small">
              {{ statusLabel(ORDER_STATUS, row.status) }}
            </el-tag>
            <el-tag v-if="row.isLocked === 1" type="success" size="small" class="lock-tag">已锁定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" @click="openDoc(row)">确认/修订</el-button>
            <el-button v-if="row.status === 'DRAFT' && row.isLocked !== 1" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="(row.status === 'DRAFT' || row.status === 'PENDING') && row.isLocked !== 1"
              link
              type="success"
              size="small"
              @click="exec(row)"
            >
              执行调拨
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无调拨单" /></template>
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
      order-type="TRANSFER"
      :order-id="docRow.id"
      :order-no="docRow.transferNo"
      :status="docRow.status"
      :is-locked="docRow.isLocked"
      :confirm-user-name="docRow.confirmUserName"
      :confirm-time="docRow.confirmTime"
      @changed="load"
    />

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑调拨单' : '新建调拨单'" width="760px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="调出仓库" required>
              <el-select v-model="form.fromWarehouse" placeholder="请选择" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入仓库" required>
              <el-select v-model="form.toWarehouse" placeholder="请选择" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="调拨明细">
          <el-button size="small" :icon="Plus" @click="addItem">添加图书</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="form.items" border size="small" style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="图书" min-width="240">
          <template #default="{ row }">
            <el-select
              v-model="row.bookId"
              filterable
              remote
              reserve-keyword
              placeholder="输入书名/ISBN 搜索"
              :remote-method="searchBooks"
              :loading="bookLoading"
              style="width: 100%"
              @change="(val) => onBookChange(row, val)"
            >
              <el-option v-for="b in bookOptions" :key="b.id" :label="`${b.bookName}（${b.isbn}）`" :value="b.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="ISBN" width="140">
          <template #default="{ row }">{{ row.isbn || '-' }}</template>
        </el-table-column>
        <el-table-column label="调拨数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :step="1" size="small" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="form.items.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="请添加调拨图书" :image-size="60" /></template>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { bookApi, stockApi, warehouseApi } from '@/api'
import OrderDocDialog from '@/components/OrderDocDialog.vue'
import { ORDER_STATUS, formatDate, statusLabel, statusType } from '@/utils/dict'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const warehouses = ref([])

const statusOptions = [
  { value: 'DRAFT', label: '待执行' },
  { value: 'PENDING', label: '待执行' },
  { value: 'TRANSFERRED', label: '已调拨' },
  { value: 'CANCELLED', label: '已作废' }
]

const query = reactive({ fromWarehouse: null, toWarehouse: null, status: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await stockApi.transferList({ ...query })
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
  query.fromWarehouse = null
  query.toWarehouse = null
  query.status = ''
  search()
}

const dialogVisible = ref(false)
const editId = ref(null)
const form = reactive({ fromWarehouse: null, toWarehouse: null, remark: '', items: [] })
const bookOptions = ref([])
const bookLoading = ref(false)

function warehouseName(id) {
  const w = warehouses.value.find((i) => i.id === id)
  return w ? w.warehouseName : id || '-'
}

function openCreate() {
  editId.value = null
  form.fromWarehouse = null
  form.toWarehouse = null
  form.remark = ''
  form.items = []
  bookOptions.value = []
  dialogVisible.value = true
}

/** 编辑仅允许草稿且未锁定（后端同样强校验） */
async function openEdit(row) {
  try {
    const data = await stockApi.transferDetail(row.id)
    const order = (data && data.order) || {}
    const items = ((data && data.items) || []).map((i) => ({
      bookId: i.bookId,
      isbn: i.isbn,
      quantity: Number(i.quantity)
    }))
    editId.value = row.id
    form.fromWarehouse = order.fromWarehouse
    form.toWarehouse = order.toWarehouse
    form.remark = order.remark || ''
    form.items = items
    bookOptions.value = items.map((i) => ({ id: i.bookId, bookName: i.isbn || '', isbn: i.isbn }))
    dialogVisible.value = true
  } catch (e) {
    /* ignore */
  }
}

function addItem() {
  form.items.push({ bookId: null, isbn: '', quantity: 1 })
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

function onBookChange(row, val) {
  const b = bookOptions.value.find((i) => i.id === val)
  if (b) row.isbn = b.isbn
}

async function save() {
  if (!form.fromWarehouse) return ElMessage.warning('请选择调出仓库')
  if (!form.toWarehouse) return ElMessage.warning('请选择调入仓库')
  if (form.fromWarehouse === form.toWarehouse) return ElMessage.warning('调出与调入仓库不能相同')
  const items = form.items.filter((i) => i.bookId && Number(i.quantity) > 0)
  if (!items.length) return ElMessage.warning('请添加至少一条调拨明细')

  saving.value = true
  try {
    const payload = {
      fromWarehouse: form.fromWarehouse,
      toWarehouse: form.toWarehouse,
      remark: form.remark,
      items: items.map((i) => ({ bookId: i.bookId, quantity: Number(i.quantity) }))
    }
    if (editId.value) {
      await stockApi.transferUpdate(editId.value, payload)
      ElMessage.success('调拨单已保存')
    } else {
      await stockApi.transferCreate(payload)
      ElMessage.success('调拨单已创建')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    /* ignore */
  } finally {
    saving.value = false
  }
}

/* ---- 确认锁定 / 修订 ---- */
const docVisible = ref(false)
const docRow = ref({})

function openDoc(row) {
  docRow.value = row
  docVisible.value = true
}

function exec(row) {
  ElMessageBox.confirm(`确认执行调拨单「${row.transferNo}」？将同步调出/调入库存。`, '操作确认', { type: 'warning' })
    .then(async () => {
      await stockApi.transferExec(row.id)
      ElMessage.success('调拨已执行')
      load()
    })
    .catch(() => {})
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