<template>
  <div class="page-container" v-loading="loading">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">
          进货单详情
          <el-tag v-if="order.status" :type="statusType(ORDER_STATUS, order.status)" class="ml-8">
            {{ statusLabel(ORDER_STATUS, order.status) }}
          </el-tag>
        </h3>
        <div>
          <el-button :icon="Picture" @click="qrVisible = true">单据二维码</el-button>
          <el-button @click="$router.back()">返回</el-button>
          <el-button v-if="order.status === 'DRAFT'" type="warning" @click="act('submit', '提交')">提交</el-button>
          <el-button v-if="order.status === 'SUBMITTED'" type="success" @click="act('instock', '入库')">确认入库</el-button>
          <el-button
            v-if="order.status && order.status !== 'CANCELLED' && order.status !== 'INSTOCK'"
            type="danger"
            @click="act('cancel', '作废')"
          >作废</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="进货单号">{{ order.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ order.supplierName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">{{ order.warehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ order.totalQty ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ formatMoney(order.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ formatMoney(order.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ formatMoney(order.paidAmount) }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">{{ formatMoney(order.discount) }}</el-descriptions-item>
        <el-descriptions-item label="结算方式">{{ SETTLE_TYPE[order.settleType] || order.settleType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经办人">{{ order.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="制单时间">{{ formatDate(order.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ formatDate(order.inTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="section-title">明细清单</h4>
      <el-table :data="order.items || []" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="bookName" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="100" align="right" />
        <el-table-column prop="price" label="进价" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <template #empty><el-empty description="无明细" /></template>
      </el-table>

      <h4 class="section-title">操作流水</h4>
      <el-timeline v-if="logs.length">
        <el-timeline-item
          v-for="log in logs"
          :key="log.id"
          :timestamp="formatDate(log.createTime)"
          placement="top"
        >
          <b>{{ log.action }}</b> · {{ log.operatorName || '-' }} <span v-if="log.remark">— {{ log.remark }}</span>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作流水" :image-size="70" />
    </div>

    <el-dialog v-model="qrVisible" title="单据二维码" width="340px" align-center>
      <div class="qr-wrap">
        <img v-if="order.orderNo" :src="`/api/qrcode/order/${order.orderNo}`" class="qr-img" alt="order qr" />
        <div class="qr-hint">内容规范：SHG:ORDER:{{ order.orderNo }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { purchaseApi } from '@/api'
import { ORDER_STATUS, SETTLE_TYPE, formatDate, formatMoney, statusLabel, statusType } from '@/utils/dict'

const route = useRoute()
const loading = ref(false)
const order = ref({})
const logs = ref([])
const qrVisible = ref(false)
const id = route.params.id

async function load() {
  loading.value = true
  try {
    order.value = (await purchaseApi.detail(id)) || {}
  } catch (e) {
    order.value = {}
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  try {
    const data = await purchaseApi.logs(id)
    logs.value = Array.isArray(data) ? data : (data && data.records) || []
  } catch (e) {
    logs.value = []
  }
}

function act(action, label) {
  ElMessageBox.confirm(`确认${label}该进货单吗？`, '操作确认', { type: 'warning' })
    .then(async () => {
      await purchaseApi[action](id)
      ElMessage.success(`${label}成功`)
      load()
      loadLogs()
    })
    .catch(() => {})
}

onMounted(() => {
  load()
  loadLogs()
})
</script>

<style scoped>
.ml-8 {
  margin-left: 8px;
}
.section-title {
  margin: 24px 0 12px;
  font-size: 15px;
  color: #1f2d3d;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
.qr-wrap {
  text-align: center;
}
.qr-img {
  width: 220px;
  height: 220px;
}
.qr-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>