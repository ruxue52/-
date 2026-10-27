<template>
  <CrudTable
    ref="tableRef"
    title="仓库管理"
    :api="warehouseApi"
    :columns="columns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-model="formModel"
    :rules="rules"
    create-text="新增仓库"
  >
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
import { warehouseApi } from '@/api'

const tableRef = ref(null)

const columns = [
  { prop: 'warehouseCode', label: '仓库编码', width: 150 },
  { prop: 'warehouseName', label: '仓库名称', minWidth: 180 },
  { prop: 'location', label: '位置', minWidth: 200 },
  { prop: 'manager', label: '负责人', width: 120 },
  { prop: 'status', label: '状态', width: 90, align: 'center' }
]

const searchFields = [{ prop: 'keyword', label: '关键字', placeholder: '编码/名称' }]

const formFields = [
  { prop: 'warehouseCode', label: '仓库编码', span: 12, required: true },
  { prop: 'warehouseName', label: '仓库名称', span: 12, required: true },
  { prop: 'location', label: '位置' },
  { prop: 'manager', label: '负责人' },
  { prop: 'status', label: '状态', type: 'switch' }
]

const rules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

function formModel() {
  return { warehouseCode: '', warehouseName: '', location: '', manager: '', status: 1 }
}
</script>