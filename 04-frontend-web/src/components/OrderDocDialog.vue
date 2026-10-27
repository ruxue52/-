<template>
  <el-dialog
    :model-value="modelValue"
    :title="`单据确认与修订 · ${orderNo || ''}`"
    width="900px"
    top="7vh"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div class="doc-head">
      <el-tag :type="locked ? 'success' : 'info'" size="small">
        {{ locked ? '已确认锁定' : '未锁定' }}
      </el-tag>
      <span v-if="locked && confirmTime" class="meta">
        确认人：{{ confirmUserName || '-' }} · {{ formatDate(confirmTime) }}
      </span>
      <span v-else-if="terminal" class="meta">单据已结束，不可修订（如需纠错请发起反向单据）</span>
      <span v-if="archived && archiveTime" class="meta">
        归档人：{{ archiveUserName || '-' }} · {{ formatDate(archiveTime) }}
      </span>
    </div>

    <div class="doc-actions">
      <el-button
        v-if="!locked && !terminal"
        type="primary"
        :icon="Lock"
        :loading="acting"
        :disabled="!canConfirm"
        @click="doConfirm"
      >
        确认锁定
      </el-button>
      <el-button
        v-if="locked && !terminal"
        type="warning"
        :icon="EditPen"
        :loading="acting"
        :disabled="!canApply"
        @click="openApply"
      >
        申请修订
      </el-button>
      <el-button
        v-if="terminal && locked && !archived && canArchive"
        type="info"
        :icon="Box"
        :loading="acting"
        @click="doArchive"
      >
        归档
      </el-button>
      <el-tag v-if="archived" type="info" size="small" effect="dark">已归档</el-tag>
      <el-tag v-if="pendingRevision" type="warning" size="small" effect="dark">存在待审批的修订申请</el-tag>
      <span v-if="!canConfirm && !locked" class="meta">当前岗位无「单据确认锁定」权限</span>
      <span v-if="!canApply && locked" class="meta">当前岗位无「修订申请」权限</span>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="修订记录" name="revisions">
        <el-table :data="revisions" border stripe size="small" style="width: 100%">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="reason" label="修订原因" min-width="170" show-overflow-tooltip />
          <el-table-column label="状态" width="95" align="center">
            <template #default="{ row }">
              <el-tag :type="revStatusType(row.status)" size="small">{{ revStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applyUserName" label="申请人" width="90" />
          <el-table-column prop="applyTime" label="申请时间" width="160">
            <template #default="{ row }">{{ formatDate(row.applyTime) }}</template>
          </el-table-column>
          <el-table-column prop="approveUserName" label="审批人" width="90" />
          <el-table-column prop="approveRemark" label="审批意见" min-width="140" show-overflow-tooltip />
          <el-table-column label="前后对比" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.beforeSnapshot"
                link
                type="primary"
                size="small"
                @click="showDiff(row)"
              >
                查看
              </el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无修订记录" :image-size="70" /></template>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="操作流水" name="logs">
        <el-table v-loading="loading" :data="actions" border stripe size="small" style="width: 100%">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="action" label="动作" width="150" />
          <el-table-column prop="operatorName" label="操作人" width="100" />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="165">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          <template #empty><el-empty description="暂无操作流水" :image-size="70" /></template>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>

  <!-- 申请修订 -->
  <el-dialog v-model="applyVisible" title="申请修订" width="520px" append-to-body destroy-on-close>
    <el-form label-width="90px">
      <el-form-item label="修订原因" required>
        <el-input v-model="applyReason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请说明需要修改的内容与原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="applyVisible = false">取消</el-button>
      <el-button type="primary" :loading="acting" @click="submitApply">提交申请</el-button>
    </template>
  </el-dialog>

  <!-- 前后快照对比 -->
  <el-dialog v-model="diffVisible" title="修订前后对比" width="900px" append-to-body destroy-on-close>
    <el-row :gutter="12">
      <el-col :span="12">
        <div class="snap-title">修改前</div>
        <pre class="snap">{{ pretty(diffRow.beforeSnapshot) }}</pre>
      </el-col>
      <el-col :span="12">
        <div class="snap-title">修改后</div>
        <pre class="snap">{{ diffRow.afterSnapshot ? pretty(diffRow.afterSnapshot) : '（尚未重新确认，暂无修改后快照）' }}</pre>
      </el-col>
    </el-row>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Lock, Box } from '@element-plus/icons-vue'
import { orderDocApi } from '@/api'
import { useUserStore } from '@/store/user'
import { formatDate } from '@/utils/dict'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  orderType: { type: String, required: true }, // IN/OUT/CHECK/TRANSFER/RETURN
  orderId: { type: [Number, String], default: null },
  orderNo: { type: String, default: '' },
  status: { type: String, default: '' },
  isLocked: { type: [Number, String], default: 0 },
  isArchived: { type: [Number, String], default: 0 },
  archiveUserName: { type: String, default: '' },
  archiveTime: { type: String, default: '' },
  confirmUserName: { type: String, default: '' },
  confirmTime: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'changed'])

const userStore = useUserStore()

const TERMINAL = {
  IN: ['INSTOCK', 'CANCELLED'],
  OUT: ['OUTSTOCK', 'CANCELLED'],
  CHECK: ['FINISHED', 'CANCELLED'],
  TRANSFER: ['TRANSFERRED', 'CANCELLED'],
  RETURN: ['FINISHED', 'CANCELLED']
}

const REV_STATUS = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已批准', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已撤销', type: 'info' },
  CLOSED: { label: '已完成', type: 'success' }
}

const tab = ref('revisions')
const acting = ref(false)
const loading = ref(false)
const revisions = ref([])
const actions = ref([])

const applyVisible = ref(false)
const applyReason = ref('')
const diffVisible = ref(false)
const diffRow = reactive({ beforeSnapshot: '', afterSnapshot: '' })

const locked = computed(() => Number(props.isLocked) === 1)
const archived = computed(() => Number(props.isArchived) === 1)
const terminal = computed(() => (TERMINAL[props.orderType] || []).includes(props.status))
const canConfirm = computed(() => userStore.isAdmin || userStore.hasPerm('order:confirm'))
const canApply = computed(() => userStore.isAdmin || userStore.hasPerm('order:revision:apply'))
const canArchive = computed(() => userStore.isAdmin || userStore.hasPerm('order:archive'))
const pendingRevision = computed(() => revisions.value.some((r) => r.status === 'PENDING'))

function revStatusLabel(s) {
  return (REV_STATUS[s] || {}).label || s
}
function revStatusType(s) {
  return (REV_STATUS[s] || {}).type || 'info'
}

async function load() {
  if (!props.orderId) return
  loading.value = true
  try {
    const [revs, logs] = await Promise.all([
      orderDocApi.revisions(props.orderType, props.orderId),
      orderDocApi.logs(props.orderType, props.orderId)
    ])
    revisions.value = Array.isArray(revs) ? revs : []
    actions.value = Array.isArray(logs) ? logs : []
  } catch (e) {
    revisions.value = []
    actions.value = []
  } finally {
    loading.value = false
  }
}

function doConfirm() {
  acting.value = true
  orderDocApi
    .confirm(props.orderType, props.orderId)
    .then(() => {
      ElMessage.success('单据已确认锁定')
      emit('changed')
      load()
    })
    .catch((e) => {
      ElMessage.error(e && e.message ? e.message : '确认失败')
    })
    .finally(() => {
      acting.value = false
    })
}

function doArchive() {
  ElMessageBox.confirm(
    '归档后该单据将从常规列表隐藏，仅可在归档页查询，且不可再修改或修订。确认归档？',
    '归档确认',
    { type: 'warning' }
  )
    .then(() => {
      acting.value = true
      return orderDocApi.archive(props.orderType, props.orderId)
    })
    .then(() => {
      ElMessage.success('单据已归档')
      emit('changed')
      load()
    })
    .catch(() => {})
    .finally(() => {
      acting.value = false
    })
}

function openApply() {
  applyReason.value = ''
  applyVisible.value = true
}

function submitApply() {
  if (!applyReason.value.trim()) return ElMessage.warning('请填写修订原因')
  acting.value = true
  orderDocApi
    .applyRevision(props.orderType, props.orderId, applyReason.value.trim())
    .then(() => {
      ElMessage.success('修订申请已提交，等待审批')
      applyVisible.value = false
      emit('changed')
      load()
    })
    .catch((e) => {
      ElMessage.error(e && e.message ? e.message : '提交失败')
    })
    .finally(() => {
      acting.value = false
    })
}

function pretty(json) {
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch (e) {
    return json || ''
  }
}

function showDiff(row) {
  diffRow.beforeSnapshot = row.beforeSnapshot
  diffRow.afterSnapshot = row.afterSnapshot
  diffVisible.value = true
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      tab.value = 'revisions'
      load()
    }
  }
)
</script>

<style scoped>
.doc-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.doc-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.meta {
  font-size: 13px;
  color: #909399;
}
.snap-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.snap {
  max-height: 460px;
  overflow: auto;
  background: #f7f8fa;
  border-radius: 4px;
  padding: 10px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
