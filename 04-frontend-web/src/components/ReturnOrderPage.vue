<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">{{ title }}</h3>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建退货单</el-button>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="退货单号/单位" clearable style="width: 180px" @keyup.enter="search" />
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
        <el-table-column prop="returnNo" label="退货单号" width="170" />
        <el-table-column prop="partnerName" label="往来单位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="totalQty" label="数量" width="90" align="right" />
        <el-table-column prop="totalAmount" label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="退货原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(ORDER_STATUS, row.status)" size="small">
              {{ statusLabel(ORDER_STATUS, row.status) }}
            </el-tag>
            <el-tag v-if="row.isLocked === 1" type="success" size="small" class="lock-tag">已锁定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="info" size="small" @click="openDoc(row)">确认/修订</el-button>
            <el-button v-if="row.status === 'DRAFT' && row.isLocked !== 1" link type="warning" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT' && row.isLocked !== 1" link type="success" size="small" @click="finish(row)">完成</el-button>
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
      order-type="RETURN"
      :order-id="docRow.id"
      :order-no="docRow.returnNo"
      :status="docRow.status"
      :is-locked="docRow.isLocked"
      :confirm-user-name="docRow.confirmUserName"
      :confirm-time="docRow.confirmTime"
      @changed="load"
    />

    <!-- 新建 / 编辑退货单 -->
    <el-dialog v-model="dialogVisible" :title="(editId ? '编辑' : '新建') + title" width="880px" top="6vh" destroy-on-close>
      <el-form :model="form" label-width="90px" class="mb-16">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="partnerLabel" required>
              <el-select v-model="form.partnerId" :placeholder="'请选择' + partnerLabel" filterable style="width: 100%">
                <el-option v-for="p in partners" :key="p.id" :label="partnerName(p)" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" required>
              <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退货原因">
              <el-input v-model="form.reason" placeholder="请输入退货原因" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" size="small" @click="bookDialog = true">添加图书</el-button>
      </div>

      <el-table :data="form.items" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="160" show-overflow-tooltip />
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" size="small" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(Number(row.quantity || 0) * Number(row.price || 0)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="form.items.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="请添加退货明细" :image-size="70" /></template>
      </el-table>

      <div class="summary-bar">
        <span>总数量：<b>{{ totalQty }}</b></span>
        <span>总金额：<b class="text-danger">{{ formatMoney(totalAmount) }}</b></span>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editId ? '保存修改' : '保存退货单' }}</el-button>
      </template>
    </el-dialog>

    <!-- 选书 -->
    <el-dialog v-model="bookDialog" title="选择图书" width="760px" append-to-body>
      <el-input v-model="bookKeyword" placeholder="输入书名 / ISBN 搜索" clearable @keyup.enter="searchBooks">
        <template #append><el-button :icon="Search" @click="searchBooks">搜索</el-button></template>
      </el-input>
      <el-table v-loading="bookLoading" :data="bookList" height="320" border stripe style="width: 100%; margin-top: 12px">
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.salePrice || row.price) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="addBook(row)">加入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { bookApi, customerApi, stockApi, supplierApi, warehouseApi } from '@/api'
import OrderDocDialog from '@/components/OrderDocDialog.vue'
import { ORDER_STATUS, formatDate, formatMoney, statusLabel, statusType } from '@/utils/dict'

const props = defineProps({
  type: { type: String, required: true }, // PURCHASE | SALES
  title: { type: String, default: '退货管理' }
})

const returnType = props.type
const partnerLabel = returnType === 'PURCHASE' ? '供应商' : '客户'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const partners = ref([])
const warehouses = ref([])

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'CANCELLED', label: '已作废' }
]

const query = reactive({ keyword: '', status: '', returnType, page: 1, size: 10 })

function partnerName(p) {
  return returnType === 'PURCHASE' ? p.supplierName : p.customerName
}

async function load() {
  loading.value = true
  try {
    const data = await stockApi.returnList({ ...query })
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
  search()
}

async function loadOptions() {
  try {
    const tasks = [warehouseApi.list({ page: 1, size: 200 })]
    tasks.push(returnType === 'PURCHASE' ? supplierApi.list({ page: 1, size: 200 }) : customerApi.list({ page: 1, size: 200 }))
    const [w, p] = await Promise.all(tasks)
    warehouses.value = (w && w.records) || []
    partners.value = (p && p.records) || []
  } catch (e) {
    /* ignore */
  }
}

/* ---- 新建 / 编辑 ---- */
const dialogVisible = ref(false)
const editId = ref(null)
const form = reactive({ partnerId: null, warehouseId: null, reason: '', items: [] })

const totalQty = computed(() => form.items.reduce((s, i) => s + Number(i.quantity || 0), 0))
const totalAmount = computed(() => form.items.reduce((s, i) => s + Number(i.quantity || 0) * Number(i.price || 0), 0))

function openCreate() {
  editId.value = null
  form.partnerId = null
  form.warehouseId = null
  form.reason = ''
  form.items = []
  dialogVisible.value = true
}

/** 编辑仅允许草稿且未锁定（后端同样强校验） */
async function openEdit(row) {
  try {
    const data = await stockApi.returnDetail(row.id)
    const order = (data && data.order) || {}
    editId.value = row.id
    form.partnerId = order.partnerId
    form.warehouseId = order.warehouseId
    form.reason = order.reason || ''
    form.items = ((data && data.items) || []).map((i) => ({
      bookId: i.bookId,
      isbn: i.isbn,
      bookName: i.bookName,
      quantity: Number(i.quantity),
      price: Number(i.price || 0)
    }))
    dialogVisible.value = true
  } catch (e) {
    /* ignore */
  }
}

const bookDialog = ref(false)
const bookKeyword = ref('')
const bookList = ref([])
const bookLoading = ref(false)

async function searchBooks() {
  bookLoading.value = true
  try {
    const data = await bookApi.list({ keyword: bookKeyword.value, page: 1, size: 20 })
    bookList.value = (data && data.records) || []
  } catch (e) {
    bookList.value = []
  } finally {
    bookLoading.value = false
  }
}

function addBook(row) {
  if (form.items.find((i) => i.bookId === row.id)) {
    ElMessage.warning('该图书已在明细中')
    return
  }
  form.items.push({
    bookId: row.id,
    isbn: row.isbn,
    bookName: row.bookName,
    quantity: 1,
    price: Number(row.salePrice || row.price || 0)
  })
}

async function save() {
  if (!form.partnerId) return ElMessage.warning(`请选择${partnerLabel}`)
  if (!form.warehouseId) return ElMessage.warning('请选择仓库')
  if (!form.items.length) return ElMessage.warning('请添加退货明细')
  saving.value = true
  try {
    const payload = {
      returnType,
      partnerId: form.partnerId,
      warehouseId: form.warehouseId,
      reason: form.reason,
      items: form.items.map((i) => ({ bookId: i.bookId, quantity: Number(i.quantity), price: Number(i.price) }))
    }
    if (editId.value) {
      await stockApi.returnUpdate(editId.value, payload)
      ElMessage.success('退货单已保存')
    } else {
      await stockApi.returnCreate(payload)
      ElMessage.success('退货单已创建')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    /* ignore */
  } finally {
    saving.value = false
  }
}

function finish(row) {
  ElMessageBox.confirm(`确认完成退货单「${row.returnNo}」吗？完成后将影响库存。`, '操作确认', { type: 'warning' })
    .then(async () => {
      await stockApi.returnFinish(row.id)
      ElMessage.success('已完成退货')
      load()
    })
    .catch(() => {})
}

function viewDetail(row) {
  stockApi
    .returnDetail(row.id)
    .then((d) => {
      ElMessageBox.alert(
        (d && d.items ? d.items.map((i) => `${i.bookName} × ${i.quantity} = ${formatMoney(i.amount)}`).join('<br/>') : '无明细'),
        `退货单 ${row.returnNo}`,
        { dangerouslyUseHTMLString: true }
      )
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

onMounted(() => {
  load()
  loadOptions()
  searchBooks()
})
</script>

<style scoped>
.summary-bar {
  display: flex;
  justify-content: flex-end;
  gap: 28px;
  margin-top: 16px;
}
.summary-bar b {
  font-size: 16px;
}
.lock-tag {
  margin-left: 4px;
}
</style>