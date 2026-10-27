<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">修订审批</h3>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-alert
        class="tip"
        type="info"
        :closable="false"
        show-icon
        title="批准后单据自动解锁，修订完成需重新确认锁定；审批人与申请人不得为同一人。"
      />

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column label="单据类型" width="100" align="center">
          <template #default="{ row }">{{ ORDER_TYPE[row.orderType] || row.orderType }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="单据号" width="180" />
        <el-table-column prop="reason" label="修订原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applyUserName" label="申请人" width="100" />
        <el-table-column prop="applyTime" label="申请时间" width="165">
          <template #default="{ row }">{{ formatDate(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="success"
              size="small"
              :disabled="!canApprove(row)"
              @click="audit(row, 'approve')"
            >
              批准
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="!canApprove(row)"
              @click="audit(row, 'reject')"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无待审批的修订申请" /></template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { orderDocApi } from '@/api'
import { useUserStore } from '@/store/user'
import { formatDate } from '@/utils/dict'

const ORDER_TYPE = { IN: '进货单', OUT: '出货单', CHECK: '盘点单', TRANSFER: '调拨单', RETURN: '退货单' }

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

/** 四眼原则：申请人不能审批自己发起的申请 */
function canApprove(row) {
  return userStore.isAdmin || row.applyUserName !== userStore.realName
}

async function load() {
  loading.value = true
  try {
    const data = await orderDocApi.pendingRevisions({ ...query })
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function audit(row, action) {
  const label = action === 'approve' ? '批准并解锁' : '驳回'
  ElMessageBox.prompt(`请输入${label}意见`, `${label}「${row.orderNo}」`, {
    inputType: 'textarea',
    inputPlaceholder: '审批意见（可空）'
  })
    .then(async ({ value }) => {
      if (action === 'approve') {
        await orderDocApi.approve(row.id, value)
        ElMessage.success('已批准，单据已解锁，请完成修订后重新确认')
      } else {
        await orderDocApi.reject(row.id, value)
        ElMessage.success('已驳回')
      }
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
</style>
