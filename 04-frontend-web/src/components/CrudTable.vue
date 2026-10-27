<template>
  <div class="page-container">
    <div class="page-card">
      <!-- 标题栏 -->
      <div class="page-header">
        <h3 class="page-title">{{ title }}</h3>
        <slot name="header-right"></slot>
      </div>

      <!-- 搜索区 -->
      <el-form v-if="searchFields.length" :inline="true" class="search-bar" @submit.prevent>
        <el-form-item
          v-for="f in searchFields"
          :key="f.prop"
          :label="f.label"
        >
          <el-input
            v-if="!f.type || f.type === 'input'"
            v-model="search[f.prop]"
            :placeholder="f.placeholder || '请输入'"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-else-if="f.type === 'select'"
            v-model="search[f.prop]"
            :placeholder="f.placeholder || '全部'"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="opt in f.options || []"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-else-if="f.type === 'daterange'"
            v-model="search[f.prop]"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具条 -->
      <div class="toolbar">
        <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreate">
          {{ createText }}
        </el-button>
        <slot name="toolbar"></slot>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        :row-key="rowKey"
        border
        stripe
        height="100%"
        style="width: 100%"
        :default-expand-all="treeMode"
        :tree-props="treeProps"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth || 110"
          :align="col.align || 'left'"
          :sortable="col.sortable || false"
          show-overflow-tooltip
        >
          <template #default="scope">
            <slot :name="'col-' + col.prop" :row="scope.row" :index="scope.$index">
              <span>{{ col.formatter ? col.formatter(scope.row) : scope.row[col.prop] }}</span>
            </slot>
          </template>
        </el-table-column>
        <el-table-column
          v-if="showActions"
          label="操作"
          :width="actionWidth"
          fixed="right"
          align="center"
        >
          <template #default="scope">
            <slot name="actions" :row="scope.row" :index="scope.$index">
              <el-button v-if="canEdit" link type="primary" size="small" @click="openEdit(scope.row)">
                编辑
              </el-button>
              <el-button v-if="canDelete" link type="danger" size="small" @click="handleRemove(scope.row)">
                删除
              </el-button>
            </slot>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <!-- 分页 -->
      <div v-if="!treeMode" class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="dialogWidth"
      destroy-on-close
      top="6vh"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="16">
          <el-col v-for="f in formFields" :key="f.prop" :span="f.span || 24">
            <el-form-item :label="f.label" :prop="f.prop">
              <slot :name="'form-' + f.prop" :form="form">
                <el-input
                  v-if="!f.type || f.type === 'input'"
                  v-model="form[f.prop]"
                  :placeholder="f.placeholder || '请输入' + f.label"
                />
                <el-input
                  v-else-if="f.type === 'textarea'"
                  v-model="form[f.prop]"
                  type="textarea"
                  :rows="3"
                  :placeholder="f.placeholder || '请输入' + f.label"
                />
                <el-input-number
                  v-else-if="f.type === 'number'"
                  v-model="form[f.prop]"
                  :min="f.min !== undefined ? f.min : 0"
                  :precision="f.precision"
                  :step="f.step || 1"
                  style="width: 100%"
                />
                <el-select
                  v-else-if="f.type === 'select'"
                  v-model="form[f.prop]"
                  :placeholder="f.placeholder || '请选择'"
                  clearable
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="opt in f.options || []"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <el-switch
                  v-else-if="f.type === 'switch'"
                  v-model="form[f.prop]"
                  :active-value="f.activeValue !== undefined ? f.activeValue : 1"
                  :inactive-value="f.inactiveValue !== undefined ? f.inactiveValue : 0"
                />
              </slot>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '数据列表' },
  api: { type: Object, required: true },
  columns: { type: Array, default: () => [] },
  searchFields: { type: Array, default: () => [] },
  formFields: { type: Array, default: () => [] },
  formModel: { type: Function, default: () => ({}) },
  rowKey: { type: String, default: 'id' },
  treeMode: { type: Boolean, default: false },
  treeProps: { type: Object, default: () => ({ children: 'children', hasChildren: 'hasChildren' }) },
  canCreate: { type: Boolean, default: true },
  canEdit: { type: Boolean, default: true },
  canDelete: { type: Boolean, default: true },
  createText: { type: String, default: '新增' },
  rules: { type: Object, default: () => ({}) },
  dialogWidth: { type: String, default: '620px' },
  extraParams: { type: Object, default: () => ({}) },
  immediate: { type: Boolean, default: true },
  transformSubmit: { type: Function, default: null }
})

const emit = defineEmits(['loaded', 'saved'])

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const dialogVisible = ref(false)
const dialogTitle = ref('新增')
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({})

const search = reactive({})

const showActions = computed(() => props.canEdit || props.canDelete)
const actionWidth = computed(() => {
  let w = 80
  if (props.canEdit) w += 60
  if (props.canDelete) w += 60
  return w
})

function initSearch() {
  props.searchFields.forEach((f) => {
    search[f.prop] = f.type === 'daterange' ? [] : f.defaultValue !== undefined ? f.defaultValue : ''
  })
}
initSearch()

function buildParams() {
  const params = { ...props.extraParams }
  Object.keys(search).forEach((key) => {
    const field = props.searchFields.find((f) => f.prop === key)
    const val = search[key]
    if (field && field.type === 'daterange') {
      if (Array.isArray(val) && val.length === 2 && val[0]) {
        params[field.startProp || 'startDate'] = val[0]
        params[field.endProp || 'endDate'] = val[1]
      }
    } else if (val !== '' && val !== null && val !== undefined) {
      params[key] = val
    }
  })
  return params
}

async function loadData() {
  if (!props.api || !props.api.list) return
  loading.value = true
  try {
    const params = buildParams()
    if (!props.treeMode) {
      params.page = page.value
      params.size = size.value
    }
    const data = await props.api.list(params)
    if (props.treeMode) {
      tableData.value = Array.isArray(data) ? data : (data && data.records) || []
      total.value = tableData.value.length
    } else {
      tableData.value = (data && data.records) || []
      total.value = (data && data.total) || 0
    }
    emit('loaded', data)
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  initSearch()
  page.value = 1
  loadData()
}

function resetForm() {
  const model = props.formModel() || {}
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(model)))
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = props.createText
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row[props.rowKey]
  dialogTitle.value = '编辑'
  resetForm()
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

async function handleSubmit() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  saving.value = true
  try {
    let payload = { ...form }
    if (props.transformSubmit) payload = props.transformSubmit(payload, !!editingId.value)
    if (editingId.value !== null && editingId.value !== undefined) {
      await props.api.update(editingId.value, payload)
    } else {
      await props.api.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    emit('saved')
    loadData()
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    saving.value = false
  }
}

function handleRemove(row) {
  const name = row.bookName || row.categoryName || row.customerName || row.supplierName ||
    row.publisherName || row.warehouseName || row.realName || row.roleName || row.username || ''
  ElMessageBox.confirm(`确认删除「${name || row[props.rowKey]}」吗？`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      await props.api.remove(row[props.rowKey])
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  if (props.immediate) loadData()
})

defineExpose({ loadData, openCreate, resetForm, form, tableData })
</script>