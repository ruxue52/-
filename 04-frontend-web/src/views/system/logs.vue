<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">操作日志</h3>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="账号/操作内容" clearable style="width: 200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="模块名" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作类型" width="130" show-overflow-tooltip />
        <el-table-column prop="method" label="请求方法" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="来源IP" width="140" />
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 || row.success === true ? 'success' : 'danger'" size="small">
              {{ row.success === 1 || row.success === true ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无日志" /></template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { systemApi } from '@/api'
import { formatDate } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])

const query = reactive({ keyword: '', module: '', startDate: '', endDate: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  const params = { ...query }
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2 && dateRange.value[0]) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  } else {
    params.startDate = ''
    params.endDate = ''
  }
  try {
    const data = await systemApi.logList(params)
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.module = ''
  dateRange.value = []
  search()
}

onMounted(load)
</script>