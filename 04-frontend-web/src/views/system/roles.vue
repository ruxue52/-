<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <div class="page-card">
          <div class="page-header">
            <h3 class="page-title">角色管理</h3>
          </div>
          <el-table
            v-loading="loading"
            :data="roles"
            border
            stripe
            style="width: 100%"
            highlight-current-row
            @current-change="onSelect"
          >
            <el-table-column type="index" label="#" width="55" align="center" />
            <el-table-column prop="roleCode" label="角色编码" width="140" />
            <el-table-column prop="roleName" label="角色名称" min-width="120" />
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无角色" /></template>
          </el-table>
        </div>
      </el-col>

      <el-col :xs="24" :lg="14">
        <div class="page-card">
          <div class="page-header">
            <h3 class="page-title">权限配置<span v-if="currentRole" class="sub">（{{ currentRole.roleName }}）</span></h3>
            <el-button
              v-if="currentRole"
              type="primary"
              :loading="saving"
              @click="savePerms"
            >
              保存权限
            </el-button>
          </div>
          <el-alert
            v-if="isAdminRole"
            class="tip"
            type="info"
            :closable="false"
            show-icon
            title="管理员角色默认拥有全部权限，无需配置"
          />
          <el-tree
            v-if="permTree.length"
            ref="treeRef"
            :data="permTree"
            :props="treeProps"
            node-key="id"
            show-checkbox
            default-expand-all
            :default-checked-keys="checkedKeys"
            :check-strictly="false"
            empty-text="暂无权限数据"
          />
          <el-empty v-else description="请选择左侧角色查看权限" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const roles = ref([])
const permTree = ref([])
const currentRole = ref(null)
const treeRef = ref(null)
const checkedKeys = ref([])

const treeProps = { children: 'children', label: 'permName' }

const isAdminRole = computed(
  () => !!currentRole.value && String(currentRole.value.roleCode).toUpperCase() === 'ADMIN'
)

async function loadRoles() {
  loading.value = true
  try {
    const data = await systemApi.roleList({ page: 1, size: 200 })
    roles.value = (data && data.records) || (Array.isArray(data) ? data : [])
  } catch (e) {
    roles.value = []
  } finally {
    loading.value = false
  }
}

async function loadPermTree() {
  try {
    const data = await systemApi.permTree()
    permTree.value = Array.isArray(data) ? data : (data && data.records) || []
  } catch (e) {
    permTree.value = []
  }
}

function onSelect(row) {
  currentRole.value = row
  checkedKeys.value = row ? row.permissionIds || (row.permissions || []).map((p) => p.id ?? p) : []
}

/** 保存角色权限：全选 + 半选一并提交，否则父级码（菜单入口）会丢失导致菜单不显示 */
async function savePerms() {
  if (!currentRole.value || !treeRef.value) return
  const ids = [
    ...treeRef.value.getCheckedKeys(false),
    ...treeRef.value.getHalfCheckedKeys()
  ]
  saving.value = true
  try {
    await systemApi.rolePermUpdate(currentRole.value.id, ids)
    ElMessage.success('权限已保存')
    const roleId = currentRole.value.id
    await loadRoles()
    const fresh = roles.value.find((r) => r.id === roleId)
    if (fresh) onSelect(fresh)
  } catch (e) {
    ElMessage.error(e && e.message ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

watch(checkedKeys, (keys) => {
  if (treeRef.value) {
    treeRef.value.setCheckedKeys(keys, false)
  }
})

onMounted(() => {
  loadRoles()
  loadPermTree()
})
</script>

<style scoped>
.sub {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}
.tip {
  margin-bottom: 12px;
}
</style>