<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">用户管理</h3>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>

      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="账号/工号/姓名" clearable style="width: 200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="username" label="登录账号" width="140" />
        <el-table-column prop="employeeNo" label="工号" width="110" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames || row.roles || []" :key="r" size="small" style="margin-right: 4px">
              {{ r.roleName || r }}
            </el-tag>
            <span v-if="!(row.roleNames || row.roles || []).length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(v) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" /></template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="form.employeeNo" placeholder="请输入工号，如 E001" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple filterable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { systemApi } from '@/api'
import { formatDate } from '@/utils/dict'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])

const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

function genderText(g) {
  return { 0: '未知', 1: '男', 2: '女' }[g] || '-'
}

async function load() {
  loading.value = true
  try {
    const data = await systemApi.userList({ ...query })
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
  query.status = ''
  search()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ username: '', employeeNo: '', password: '', realName: '', phone: '', email: '', gender: 0, roleIds: [] })

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增用户'
  Object.assign(form, { username: '', employeeNo: '', password: '', realName: '', phone: '', email: '', gender: 0, roleIds: [] })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    username: row.username,
    employeeNo: row.employeeNo || '',
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    gender: row.gender ?? 0,
    roleIds: row.roleIds || (row.roles || []).map((r) => r.id || r.roleId)
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      username: form.username,
      employeeNo: form.employeeNo,
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      gender: form.gender,
      roleIds: form.roleIds
    }
    if (editingId.value) {
      await systemApi.userUpdate(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      payload.password = form.password
      await systemApi.userCreate(payload)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    /* ignore */
  } finally {
    saving.value = false
  }
}

function toggleStatus(row, v) {
  systemApi
    .userStatus(row.id, v)
    .then(() => {
      row.status = v
      ElMessage.success(v === 1 ? '已启用' : '已停用')
    })
    .catch(() => {})
}

function resetPwd(row) {
  ElMessageBox.prompt(`为用户「${row.realName || row.username}」设置新密码`, '重置密码', {
    inputType: 'password',
    inputPattern: /.{6,}/,
    inputErrorMessage: '密码至少 6 位'
  })
    .then(async ({ value }) => {
      await systemApi.userResetPwd(row.id, value)
      ElMessage.success('密码已重置')
    })
    .catch(() => {})
}

function remove(row) {
  ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '操作确认', { type: 'warning' })
    .then(async () => {
      await systemApi.userRemove(row.id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

async function loadRoles() {
  try {
    const data = await systemApi.roleList({ page: 1, size: 200 })
    roles.value = (data && data.records) || (Array.isArray(data) ? data : [])
  } catch (e) {
    /* ignore */
  }
}

onMounted(() => {
  load()
  loadRoles()
})
</script>