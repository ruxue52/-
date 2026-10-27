<template>
  <CrudTable
    ref="tableRef"
    title="图书分类"
    :api="categoryApi"
    :columns="columns"
    :form-fields="formFields"
    :form-model="formModel"
    :rules="rules"
    create-text="新增分类"
    tree-mode
    :tree-props="{ children: 'children' }"
    @loaded="onLoaded"
  >
    <template #col-status="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
        {{ row.status === 1 ? '启用' : '停用' }}
      </el-tag>
    </template>
    <template #col-parentId="{ row }">
      {{ nameOf(row.parentId) }}
    </template>
  </CrudTable>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import { categoryApi } from '@/api'

const tableRef = ref(null)
const flatList = ref([])

const columns = [
  { prop: 'categoryName', label: '分类名称', minWidth: 200 },
  { prop: 'categoryCode', label: '分类编码', width: 160 },
  { prop: 'parentId', label: '上级分类', width: 160 },
  { prop: 'sortNo', label: '排序', width: 90, align: 'center' },
  { prop: 'status', label: '状态', width: 90, align: 'center' }
]

const formFields = computed(() => [
  { prop: 'categoryName', label: '分类名称', required: true },
  { prop: 'categoryCode', label: '分类编码' },
  { prop: 'parentId', label: '上级分类', type: 'select', options: parentOptions.value },
  { prop: 'sortNo', label: '排序', type: 'number' },
  { prop: 'status', label: '状态', type: 'switch' }
])

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

function formModel() {
  return { categoryName: '', categoryCode: '', parentId: 0, sortNo: 0, status: 1 }
}

function flatten(tree, out = [], prefix = '') {
  ;(tree || []).forEach((n) => {
    out.push({ id: n.id, name: prefix + n.categoryName, value: n.id, label: prefix + n.categoryName })
    if (n.children && n.children.length) flatten(n.children, out, prefix + n.categoryName + ' / ')
  })
  return out
}

const parentOptions = computed(() => [
  { value: 0, label: '— 顶级分类 —' },
  ...flatList.value.map((i) => ({ value: i.id, label: i.name }))
])

function nameOf(id) {
  if (!id || id === 0) return '顶级'
  const f = flatList.value.find((i) => i.id === id)
  return f ? f.name : id
}

function onLoaded(data) {
  flatList.value = flatten(Array.isArray(data) ? data : (data && data.records) || [])
}
</script>