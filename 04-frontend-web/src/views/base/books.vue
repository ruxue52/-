<template>
  <CrudTable
    ref="tableRef"
    title="图书管理"
    :api="bookApi"
    :columns="columns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-model="formModel"
    :rules="rules"
    create-text="新增图书"
    dialog-width="720px"
  >
    <template #toolbar>
      <el-button :icon="Refresh" @click="addOptions">刷新选项</el-button>
    </template>

    <template #col-status="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
        {{ row.status === 1 ? '在售' : '停售' }}
      </el-tag>
    </template>

    <template #col-price="{ row }">
      {{ formatMoney(row.price) }}
    </template>
    <template #col-salePrice="{ row }">
      <span class="text-danger">{{ formatMoney(row.salePrice) }}</span>
    </template>

    <template #actions="{ row }">
      <el-button link type="primary" size="small" @click="openQrcode(row)">二维码</el-button>
      <el-button link type="primary" size="small" @click="tableRef.openEdit(row)">编辑</el-button>
      <el-button link type="danger" size="small" @click="removeBook(row)">删除</el-button>
    </template>
  </CrudTable>

  <!-- 二维码弹窗 -->
  <el-dialog v-model="qrVisible" title="图书二维码" width="360px" align-center>
    <div class="qr-wrap">
      <img v-if="qrIsbn" :src="qrSrc" class="qr-img" :alt="qrIsbn" @error="qrError = true" />
      <el-empty v-if="qrError" description="二维码加载失败（请确认后端已放行 /qrcode/**）" :image-size="90" />
      <div class="qr-isbn">ISBN：{{ qrIsbn }}</div>
      <div class="qr-hint">内容规范：SHG:BOOK:{{ qrIsbn }}</div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import CrudTable from '@/components/CrudTable.vue'
import { bookApi, categoryApi, publisherApi } from '@/api'
import { formatMoney } from '@/utils/dict'

const tableRef = ref(null)
const categoryOptions = ref([])
const publisherOptions = ref([])

const columns = [
  { prop: 'isbn', label: 'ISBN', width: 140 },
  { prop: 'bookName', label: '书名', minWidth: 180 },
  { prop: 'author', label: '作者', width: 120 },
  { prop: 'categoryName', label: '分类', width: 110 },
  { prop: 'publisherName', label: '出版社', width: 140 },
  { prop: 'price', label: '定价', width: 90, align: 'right' },
  { prop: 'salePrice', label: '零售价', width: 90, align: 'right' },
  { prop: 'purchasePrice', label: '参考进价', width: 100, align: 'right' },
  { prop: 'unit', label: '单位', width: 70, align: 'center' },
  { prop: 'shelfNo', label: '货架位', width: 100 },
  { prop: 'status', label: '状态', width: 80, align: 'center' }
]

const searchFields = computed(() => [
  { prop: 'keyword', label: '关键字', placeholder: '书名/ISBN/作者' },
  { prop: 'categoryId', label: '分类', type: 'select', options: categoryOptions.value },
  { prop: 'publisherId', label: '出版社', type: 'select', options: publisherOptions.value },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { value: 1, label: '在售' },
      { value: 0, label: '停售' }
    ]
  }
])

const formFields = computed(() => [
  { prop: 'isbn', label: 'ISBN', required: true },
  { prop: 'bookName', label: '书名', span: 16, required: true },
  { prop: 'author', label: '作者', span: 8 },
  { prop: 'categoryId', label: '分类', type: 'select', span: 12, options: categoryOptions.value },
  { prop: 'publisherId', label: '出版社', type: 'select', span: 12, options: publisherOptions.value },
  { prop: 'price', label: '定价', type: 'number', precision: 2, span: 8 },
  { prop: 'purchasePrice', label: '参考进价', type: 'number', precision: 2, span: 8 },
  { prop: 'salePrice', label: '零售价', type: 'number', precision: 2, span: 8 },
  { prop: 'unit', label: '单位', span: 8 },
  { prop: 'shelfNo', label: '货架位', span: 8 },
  { prop: 'publishDate', label: '出版日期', placeholder: 'YYYY-MM', span: 8 },
  { prop: 'summary', label: '简介', type: 'textarea' },
  { prop: 'status', label: '状态', type: 'switch' }
])

const rules = {
  isbn: [{ required: true, message: '请输入 ISBN', trigger: 'blur' }],
  bookName: [{ required: true, message: '请输入书名', trigger: 'blur' }]
}

function formModel() {
  return {
    isbn: '',
    bookName: '',
    author: '',
    publisherId: null,
    categoryId: null,
    publishDate: '',
    price: 0,
    purchasePrice: 0,
    salePrice: 0,
    unit: '册',
    shelfNo: '',
    summary: '',
    status: 1
  }
}

function flatCategories(tree, out = [], prefix = '') {
  ;(tree || []).forEach((node) => {
    out.push({ value: node.id, label: prefix + node.categoryName })
    if (node.children && node.children.length) {
      flatCategories(node.children, out, prefix + node.categoryName + ' / ')
    }
  })
  return out
}

async function loadOptions() {
  try {
    const [cats, pubs] = await Promise.all([categoryApi.list({}), publisherApi.list({ page: 1, size: 200 })])
    categoryOptions.value = flatCategories(Array.isArray(cats) ? cats : (cats && cats.records) || [])
    publisherOptions.value = ((pubs && pubs.records) || []).map((p) => ({
      value: p.id,
      label: p.publisherName
    }))
  } catch (e) {
    /* ignore */
  }
}

function addOptions() {
  loadOptions()
  tableRef.value && tableRef.value.loadData()
}

const qrVisible = ref(false)
const qrIsbn = ref('')
const qrError = ref(false)
const qrSrc = computed(() => `/api/qrcode/book/${qrIsbn.value}`)

function openQrcode(row) {
  qrIsbn.value = row.isbn
  qrError.value = false
  qrVisible.value = true
}

function removeBook(row) {
  ElMessageBox.confirm(`确认删除图书「${row.bookName}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await bookApi.remove(row.id)
      ElMessage.success('删除成功')
      tableRef.value.loadData()
    })
    .catch(() => {})
}

onMounted(loadOptions)
</script>

<style scoped>
.qr-wrap {
  text-align: center;
}
.qr-img {
  width: 220px;
  height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.qr-isbn {
  margin-top: 12px;
  font-weight: 600;
}
.qr-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>