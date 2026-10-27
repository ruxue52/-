<template>
  <CrudTable
    ref="tableRef"
    title="供应商管理"
    :api="supplierApi"
    :columns="columns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-model="formModel"
    :rules="rules"
    create-text="新增供应商"
  >
    <template #col-settleType="{ row }">{{ SETTLE_TYPE[row.settleType] || row.settleType }}</template>
    <template #col-creditLimit="{ row }">{{ formatMoney(row.creditLimit) }}</template>
    <template #col-status="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
        {{ row.status === 1 ? '启用' : '停用' }}
      </el-tag>
    </template>
  </CrudTable>
</template>

<script setup>
import { ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import { supplierApi } from '@/api'
import { SETTLE_TYPE, formatMoney, toOptions } from '@/utils/dict'

const tableRef = ref(null)

const columns = [
  { prop: 'supplierCode', label: '供应商编码', width: 140 },
  { prop: 'supplierName', label: '供应商名称', minWidth: 200 },
  { prop: 'contact', label: '联系人', width: 110 },
  { prop: 'phone', label: '电话', width: 140 },
  { prop: 'settleType', label: '结算方式', width: 110, align: 'center' },
  { prop: 'creditLimit', label: '授信额度', width: 120, align: 'right' },
  { prop: 'address', label: '地址', minWidth: 180 },
  { prop: 'status', label: '状态', width: 90, align: 'center' }
]

const searchFields = [
  { prop: 'keyword', label: '关键字', placeholder: '编码/名称' },
  { prop: 'settleType', label: '结算方式', type: 'select', options: toOptions(SETTLE_TYPE) }
]

const formFields = [
  { prop: 'supplierCode', label: '供应商编码', span: 12, required: true },
  { prop: 'supplierName', label: '供应商名称', span: 12, required: true },
  { prop: 'contact', label: '联系人', span: 12 },
  { prop: 'phone', label: '电话', span: 12 },
  { prop: 'settleType', label: '结算方式', type: 'select', span: 12, options: toOptions(SETTLE_TYPE) },
  { prop: 'creditLimit', label: '授信额度', type: 'number', precision: 2, span: 12 },
  { prop: 'address', label: '地址' },
  { prop: 'status', label: '状态', type: 'switch' }
]

const rules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

function formModel() {
  return {
    supplierCode: '',
    supplierName: '',
    contact: '',
    phone: '',
    address: '',
    settleType: 'CASH',
    creditLimit: 0,
    status: 1
  }
}
</script>