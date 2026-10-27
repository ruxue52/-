<template>
  <CrudTable
    ref="tableRef"
    title="客户 / 会员管理"
    :api="customerApi"
    :columns="columns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-model="formModel"
    :rules="rules"
    create-text="新增客户"
    dialog-width="700px"
  >
    <template #col-customerType="{ row }">
      <el-tag :type="(CUSTOMER_TYPE[row.customerType] || {}).type || 'info'" size="small">
        {{ (CUSTOMER_TYPE[row.customerType] || {}).label || row.customerType }}
      </el-tag>
    </template>
    <template #col-discount="{ row }">{{ row.discount }}%</template>
    <template #col-balance="{ row }">{{ formatMoney(row.balance) }}</template>
    <template #col-status="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
        {{ row.status === 1 ? '启用' : '停用' }}
      </el-tag>
    </template>
  </CrudTable>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import { customerApi, memberLevelApi } from '@/api'
import { CUSTOMER_TYPE, formatMoney, toOptions } from '@/utils/dict'

const tableRef = ref(null)
const levelOptions = ref([])

const columns = [
  { prop: 'customerCode', label: '客户编码', width: 130 },
  { prop: 'customerName', label: '客户名称', minWidth: 160 },
  { prop: 'customerType', label: '类型', width: 90, align: 'center' },
  { prop: 'phone', label: '电话', width: 140 },
  { prop: 'levelName', label: '会员等级', width: 110 },
  { prop: 'discount', label: '折扣', width: 80, align: 'center' },
  { prop: 'points', label: '积分', width: 90, align: 'right' },
  { prop: 'balance', label: '余额', width: 110, align: 'right' },
  { prop: 'status', label: '状态', width: 80, align: 'center' }
]

const searchFields = [
  { prop: 'keyword', label: '关键字', placeholder: '编码/名称' },
  { prop: 'customerType', label: '类型', type: 'select', options: toOptions(CUSTOMER_TYPE) },
  { prop: 'phone', label: '电话', placeholder: '手机号' }
]

const formFields = computed(() => [
  { prop: 'customerCode', label: '客户编码', span: 12, required: true },
  { prop: 'customerName', label: '客户名称', span: 12, required: true },
  { prop: 'customerType', label: '客户类型', type: 'select', span: 12, options: toOptions(CUSTOMER_TYPE) },
  { prop: 'levelId', label: '会员等级', type: 'select', span: 12, options: levelOptions.value },
  { prop: 'phone', label: '电话', span: 12 },
  { prop: 'discount', label: '折扣(%)', type: 'number', precision: 2, span: 12 },
  { prop: 'points', label: '积分', type: 'number', span: 12 },
  { prop: 'balance', label: '余额', type: 'number', precision: 2, span: 12 },
  { prop: 'address', label: '地址' },
  { prop: 'status', label: '状态', type: 'switch' }
])

const rules = {
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

function formModel() {
  return {
    customerCode: '',
    customerName: '',
    customerType: 'RETAIL',
    phone: '',
    address: '',
    levelId: null,
    discount: 100,
    points: 0,
    balance: 0,
    status: 1
  }
}

async function loadLevels() {
  try {
    const data = await memberLevelApi.list({})
    const arr = Array.isArray(data) ? data : (data && data.records) || []
    levelOptions.value = arr.map((l) => ({ value: l.id, label: l.levelName }))
  } catch (e) {
    /* ignore */
  }
}

onMounted(loadLevels)
</script>