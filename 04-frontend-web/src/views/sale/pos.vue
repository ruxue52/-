<template>
  <div class="page-container pos-page">
    <el-row :gutter="16">
      <!-- 左：检索 -->
      <el-col :xs="24" :lg="14">
        <div class="page-card pos-left">
          <div class="page-header">
            <h3 class="page-title">POS 收银 · 商品检索</h3>
            <el-tag type="info" size="small">扫码枪可直接扫描 ISBN 回车加入</el-tag>
          </div>

          <el-input
            ref="searchInput"
            v-model="keyword"
            size="large"
            placeholder="扫描/输入 ISBN、书名、拼音码后回车"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #append><el-button type="primary" @click="doSearch">检索</el-button></template>
          </el-input>

          <el-table
            v-loading="searching"
            :data="results"
            height="440"
            border
            stripe
            style="width: 100%; margin-top: 12px"
            highlight-current-row
            @row-dblclick="addToCart"
          >
            <el-table-column prop="isbn" label="ISBN" width="140" />
            <el-table-column prop="bookName" label="书名" min-width="170" show-overflow-tooltip />
            <el-table-column label="售价" width="100" align="right">
              <template #default="{ row }">{{ formatMoney(salePriceOf(row)) }}</template>
            </el-table-column>
            <el-table-column label="可用库存" width="100" align="right">
              <template #default="{ row }">
                <span :class="{ 'text-danger': stockOf(row) <= 0 }">{{ stockOf(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="addToCart(row)">加入</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="输入关键字开始检索" :image-size="80" /></template>
          </el-table>
        </div>
      </el-col>

      <!-- 右：购物车 -->
      <el-col :xs="24" :lg="10">
        <div class="page-card pos-right">
          <div class="page-header">
            <h3 class="page-title">购物车（{{ cart.length }}）</h3>
            <el-button link type="danger" :disabled="!cart.length" @click="cart = []">清空</el-button>
          </div>

          <el-form :model="order" label-width="80px" size="default" class="mb-16">
            <el-form-item label="客户">
              <el-select v-model="order.customerId" placeholder="散客（可空）" clearable filterable style="width: 100%">
                <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="出货仓库" required>
              <el-select v-model="order.warehouseId" placeholder="请选择仓库" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="支付方式">
              <el-radio-group v-model="order.paymentType">
                <el-radio-button v-for="o in payOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>

          <el-table :data="cart" border size="small" height="260" style="width: 100%">
            <el-table-column prop="bookName" label="商品" min-width="140" show-overflow-tooltip />
            <el-table-column label="数量" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" style="width: 90px" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0" :precision="2" size="small" controls-position="right" style="width: 90px" />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="90" align="right">
              <template #default="{ row }">{{ formatMoney(row.quantity * row.price) }}</template>
            </el-table-column>
            <el-table-column label="" width="50" align="center">
              <template #default="{ $index }">
                <el-icon class="del-icon" @click="cart.splice($index, 1)"><Delete /></el-icon>
              </template>
            </el-table-column>
            <template #empty><el-empty description="购物车为空" :image-size="70" /></template>
          </el-table>

          <div class="pos-summary">
            <div class="sum-row">
              <span>商品件数</span><b>{{ totalQty }}</b>
            </div>
            <div class="sum-row">
              <span>商品金额</span><b>{{ formatMoney(totalAmount) }}</b>
            </div>
            <div class="sum-row">
              <span>优惠金额</span>
              <el-input-number v-model="order.discount" :min="0" :precision="2" size="small" style="width: 120px" />
            </div>
            <div class="sum-row pay-row">
              <span>应收金额</span><b class="pay-amount">{{ formatMoney(payAmount) }}</b>
            </div>
            <div class="sum-row">
              <span>实收金额</span>
              <el-input-number v-model="order.payAmount" :min="0" :precision="2" size="small" style="width: 120px" />
            </div>
          </div>

          <div class="pos-actions">
            <el-button size="large" :disabled="!canSubmit" :loading="saving" @click="submit(false)">保存草稿</el-button>
            <el-button type="success" size="large" :disabled="!canSubmit" :loading="saving" @click="submit(true)">
              结算并出库
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Search } from '@element-plus/icons-vue'
import { customerApi, saleApi, warehouseApi } from '@/api'
import { PAYMENT_TYPE, formatMoney, toOptions } from '@/utils/dict'

const router = useRouter()
const keyword = ref('')
const searchInput = ref(null)
const results = ref([])
const searching = ref(false)
const cart = ref([])
const customers = ref([])
const warehouses = ref([])
const saving = ref(false)

const payOptions = toOptions(PAYMENT_TYPE)

const order = reactive({
  customerId: null,
  warehouseId: null,
  paymentType: 'CASH',
  discount: 0,
  payAmount: 0
})

function salePriceOf(row) {
  return Number(row.salePrice ?? row.price ?? 0)
}

function stockOf(row) {
  return Number(row.quantity ?? row.stockQty ?? row.stock ?? 0)
}

const totalQty = computed(() => cart.value.reduce((s, i) => s + Number(i.quantity || 0), 0))
const totalAmount = computed(() => cart.value.reduce((s, i) => s + Number(i.quantity || 0) * Number(i.price || 0), 0))
const payAmount = computed(() => Math.max(0, totalAmount.value - Number(order.discount || 0)))
const canSubmit = computed(() => cart.value.length > 0)

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  searching.value = true
  try {
    const data = await saleApi.posSearch(kw)
    const arr = Array.isArray(data) ? data : (data && data.records) || []
    results.value = arr
    // 扫码枪：唯一命中则自动加入购物车
    if (arr.length === 1) {
      addToCart(arr[0])
      keyword.value = ''
      results.value = []
      focusSearch()
    }
  } catch (e) {
    results.value = []
  } finally {
    searching.value = false
  }
}

function focusSearch() {
  if (searchInput.value) searchInput.value.focus()
}

function addToCart(row) {
  const bookId = row.bookId || row.id
  const stock = stockOf(row)
  const exist = cart.value.find((i) => i.bookId === bookId)
  if (exist) {
    if (exist.quantity + 1 > stock) {
      ElMessage.warning('库存不足，无法继续增加')
      return
    }
    exist.quantity += 1
  } else {
    if (stock <= 0) {
      ElMessage.warning('该商品库存不足')
      return
    }
    cart.value.push({
      bookId,
      isbn: row.isbn,
      bookName: row.bookName,
      quantity: 1,
      price: salePriceOf(row),
      stock
    })
  }
  order.payAmount = Number(payAmount.value.toFixed(2))
}

async function submit(withOutstock) {
  if (!order.warehouseId) return ElMessage.warning('请选择出货仓库')
  if (!cart.value.length) return ElMessage.warning('购物车为空')

  const doIt = async () => {
    saving.value = true
    try {
      const payload = {
        customerId: order.customerId || null,
        warehouseId: order.warehouseId,
        paymentType: order.paymentType,
        discount: Number(order.discount || 0),
        payAmount: Number(order.payAmount || 0),
        items: cart.value.map((i) => ({ bookId: i.bookId, quantity: Number(i.quantity), price: Number(i.price) }))
      }
      const data = await saleApi.create(payload)
      const id = data && (data.id || data.orderId)
      if (withOutstock && id) {
        await saleApi.outstock(id)
        ElMessage.success('结算出库成功')
      } else {
        ElMessage.success('出货单已保存')
      }
      cart.value = []
      order.discount = 0
      order.payAmount = 0
      keyword.value = ''
      results.value = []
      focusSearch()
      if (id) router.push(`/sale/detail/${id}`)
    } catch (e) {
      /* ignore */
    } finally {
      saving.value = false
    }
  }

  if (withOutstock) {
    ElMessageBox.confirm(`确认结算？应收 ¥${formatMoney(payAmount.value)}`, '收银结算', {
      type: 'warning',
      confirmButtonText: '确认结算'
    })
      .then(doIt)
      .catch(() => {})
  } else {
    doIt()
  }
}

async function loadOptions() {
  try {
    const [c, w] = await Promise.all([customerApi.list({ page: 1, size: 200 }), warehouseApi.list({ page: 1, size: 200 })])
    customers.value = (c && c.records) || []
    warehouses.value = (w && w.records) || []
    if (warehouses.value.length && !order.warehouseId) order.warehouseId = warehouses.value[0].id
  } catch (e) {
    /* ignore */
  }
}

onMounted(() => {
  loadOptions()
  focusSearch()
})
</script>

<style scoped>
.pos-left,
.pos-right {
  min-height: 560px;
}
.pos-summary {
  margin-top: 16px;
  border-top: 1px dashed #ebeef5;
  padding-top: 12px;
}
.sum-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}
.pay-row {
  font-size: 16px;
  border-top: 1px solid #ebeef5;
  margin-top: 6px;
  padding-top: 10px;
}
.pay-amount {
  color: #f56c6c;
  font-size: 24px;
}
.pos-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
.pos-actions .el-button {
  flex: 1;
}
.del-icon {
  cursor: pointer;
  color: #f56c6c;
}
</style>