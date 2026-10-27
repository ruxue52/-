<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">进货开单</h3>
        <div>
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary" :loading="saving" :icon="Check" @click="handleSave">保存草稿</el-button>
        </div>
      </div>

      <el-form :model="form" label-width="90px" class="mb-16">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="供应商" required>
              <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable style="width: 100%">
                <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="入库仓库" required>
              <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="结算方式">
              <el-select v-model="form.settleType" style="width: 100%">
                <el-option v-for="o in settleOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="优惠金额">
              <el-input-number v-model="form.discount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="已付金额">
              <el-input-number v-model="form.paidAmount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="18">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="bookDialog = true">添加图书</el-button>
        <el-button :icon="Delete" :disabled="!form.items.length" @click="form.items = []">清空明细</el-button>
      </div>

      <el-table :data="form.items" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column label="数量" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="进价" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" size="small" style="width: 130px" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(rowAmount(row)) }}</template>
        </el-table-column>
        <el-table-column label="批次号" width="130">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="form.items.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="请添加进货图书明细" /></template>
      </el-table>

      <div class="summary-bar">
        <span>品种数：<b>{{ form.items.length }}</b></span>
        <span>总数量：<b>{{ totalQty }}</b></span>
        <span>总金额：<b>{{ formatMoney(totalAmount) }}</b></span>
        <span>应付金额：<b class="text-danger">{{ formatMoney(payAmount) }}</b></span>
      </div>
    </div>

    <!-- 选书弹窗 -->
    <el-dialog v-model="bookDialog" title="选择图书" width="820px">
      <el-input v-model="bookKeyword" placeholder="输入书名 / ISBN / 作者搜索" clearable @keyup.enter="searchBooks">
        <template #append><el-button :icon="Search" @click="searchBooks">搜索</el-button></template>
      </el-input>
      <el-table v-loading="bookLoading" :data="bookList" height="360" border stripe style="width: 100%; margin-top: 12px">
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column label="定价" width="90" align="right">
          <template #default="{ row }">{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="addBook(row)">加入</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="未找到图书" /></template>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="bookPage"
          :page-size="bookSize"
          :total="bookTotal"
          layout="total, prev, pager, next"
          background
          @current-change="searchBooks"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Delete, Plus, Search } from '@element-plus/icons-vue'
import { bookApi, purchaseApi, supplierApi, warehouseApi } from '@/api'
import { SETTLE_TYPE, formatMoney, toOptions } from '@/utils/dict'

const router = useRouter()
const saving = ref(false)

const suppliers = ref([])
const warehouses = ref([])
const settleOptions = toOptions(SETTLE_TYPE)

const form = reactive({
  supplierId: null,
  warehouseId: null,
  settleType: 'CASH',
  discount: 0,
  paidAmount: 0,
  remark: '',
  items: []
})

const bookDialog = ref(false)
const bookKeyword = ref('')
const bookList = ref([])
const bookLoading = ref(false)
const bookPage = ref(1)
const bookSize = ref(10)
const bookTotal = ref(0)

function rowAmount(row) {
  return Number(row.quantity || 0) * Number(row.price || 0)
}

const totalQty = computed(() => form.items.reduce((s, i) => s + Number(i.quantity || 0), 0))
const totalAmount = computed(() => form.items.reduce((s, i) => s + rowAmount(i), 0))
const payAmount = computed(() => Math.max(0, totalAmount.value - Number(form.discount || 0)))

async function loadOptions() {
  try {
    const [s, w] = await Promise.all([supplierApi.list({ page: 1, size: 200 }), warehouseApi.list({ page: 1, size: 200 })])
    suppliers.value = (s && s.records) || []
    warehouses.value = (w && w.records) || []
  } catch (e) {
    /* ignore */
  }
}

async function searchBooks() {
  bookLoading.value = true
  try {
    const data = await bookApi.list({ keyword: bookKeyword.value, page: bookPage.value, size: bookSize.value })
    bookList.value = (data && data.records) || []
    bookTotal.value = (data && data.total) || 0
  } catch (e) {
    bookList.value = []
  } finally {
    bookLoading.value = false
  }
}

function addBook(row) {
  const exist = form.items.find((i) => i.bookId === row.id)
  if (exist) {
    exist.quantity += 1
    ElMessage.success('已累加数量')
    return
  }
  form.items.push({
    bookId: row.id,
    isbn: row.isbn,
    bookName: row.bookName,
    quantity: 1,
    price: Number(row.purchasePrice || row.price || 0),
    batchNo: '',
    remark: ''
  })
  ElMessage.success('已加入明细')
}

async function handleSave() {
  if (!form.supplierId) return ElMessage.warning('请选择供应商')
  if (!form.warehouseId) return ElMessage.warning('请选择入库仓库')
  if (!form.items.length) return ElMessage.warning('请添加进货明细')
  saving.value = true
  try {
    const payload = {
      supplierId: form.supplierId,
      warehouseId: form.warehouseId,
      settleType: form.settleType,
      discount: Number(form.discount || 0),
      paidAmount: Number(form.paidAmount || 0),
      remark: form.remark,
      items: form.items.map((i) => ({
        bookId: i.bookId,
        quantity: Number(i.quantity),
        price: Number(i.price),
        batchNo: i.batchNo,
        remark: i.remark
      }))
    }
    const data = await purchaseApi.create(payload)
    ElMessage.success('进货单已保存')
    const id = data && (data.id || data.orderId)
    router.replace(id ? `/purchase/detail/${id}` : '/purchase/orders')
  } catch (e) {
    /* ignore */
  } finally {
    saving.value = false
  }
}

onMounted(() => {
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
  font-size: 14px;
}
.summary-bar b {
  font-size: 16px;
}
</style>