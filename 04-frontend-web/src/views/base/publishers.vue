<template>
  <CrudTable
    ref="tableRef"
    title="出版社管理"
    :api="publisherApi"
    :columns="columns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-model="formModel"
    :rules="rules"
    create-text="新增出版社"
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
import { publisherApi } from '@/api'

const tableRef = ref(null)

const columns = [
  { prop: 'publisherName', label: '出版社名称', minWidth: 200 },
  { prop: 'contact', label: '联系人', width: 120 },
  { prop: 'phone', label: '联系电话', width: 150 },
  { prop: 'address', label: '地址', minWidth: 200 },
  { prop: 'status', label: '状态', width: 90, align: 'center' }
]

const searchFields = [
  { prop: 'keyword', label: '关键字', placeholder: '出版社名称' }
]

const formFields = [
  { prop: 'publisherName', label: '出版社名称', required: true },
  { prop: 'contact', label: '联系人' },
  { prop: 'phone', label: '联系电话' },
  { prop: 'address', label: '地址' },
  { prop: 'status', label: '状态', type: 'switch' }
]

const rules = {
  publisherName: [{ required: true, message: '请输入出版社名称', trigger: 'blur' }]
}

function formModel() {
  return { publisherName: '', contact: '', phone: '', address: '', status: 1 }
}
</script>