<template>
  <view class="container">
    <!-- 盘点单选择 -->
    <block v-if="!checkId">
      <view class="card">
        <view class="row">
          <view class="card-title" style="margin:0">选择盘点单</view>
          <view class="spacer"></view>
          <text class="muted" @click="loadOrders">刷新</text>
        </view>
        <view class="order-item" v-for="o in orders" :key="o.id" @click="selectOrder(o.id)">
          <view class="oi-left">
            <text class="oi-no">{{ o.checkNo }}</text>
            <text class="oi-sub">{{ o.warehouseName || ('仓库' + o.warehouseId) }} · {{ typeText(o.checkType) }} · 品种 {{ o.totalItems }}</text>
          </view>
          <text class="oi-status" :class="statusCls(o.status)">{{ statusText(o.status) }}</text>
        </view>
        <view v-if="!orders.length" class="muted center">暂无盘点单</view>
      </view>
    </block>

    <!-- 盘点录入 -->
    <block v-else>
      <view class="card">
        <view class="row">
          <view class="card-title" style="margin:0">{{ detail.checkNo }}</view>
          <view class="spacer"></view>
          <text class="muted" @click="backToList">切换单</text>
        </view>
        <view class="muted">{{ detail.warehouseName || ('仓库' + detail.warehouseId) }} · 已录差异 {{ diffCount }} 项 / 共 {{ items.length }} 项</view>
        <button class="scan-btn mt16" @click="scan">📷 扫码录入</button>
        <view class="manual mt8">
          <input class="input" v-model="manualCode" placeholder="或输入 ISBN" />
          <button class="btn-add" @click="scanManual">录入</button>
        </view>
      </view>

      <view class="card">
        <view class="card-title">盘点明细</view>
        <view class="item-row" v-for="(it, i) in items" :key="i">
          <view class="it-left">
            <text class="it-name">{{ it.bookName }}</text>
            <text class="it-isbn">{{ it.isbn }}</text>
          </view>
          <view class="it-right">
            <text class="it-qty">账{{ it.bookQty }}</text>
            <input class="it-input" type="number" v-model="it.realQty" />
            <text class="it-diff" :class="{ diff: it.realQty - it.bookQty !== 0 }">{{ it.realQty - it.bookQty }}</text>
          </view>
        </view>
        <view v-if="!items.length" class="muted center">该盘点单暂无明细</view>
      </view>

      <view class="footer">
        <button class="btn-save" @click="saveItems">保存实盘</button>
        <button class="btn-finish" @click="finish">完成盘点</button>
      </view>
    </block>
  </view>
</template>

<script>
import { request, requireLogin, recordsOf, extractIsbn } from '@/utils/request.js'

export default {
  data() {
    return {
      orders: [],
      checkId: null,
      detail: {},
      items: [],
      manualCode: ''
    }
  },
  computed: {
    diffCount() {
      return this.items.filter((it) => Number(it.realQty) - Number(it.bookQty) !== 0).length
    }
  },
  onShow() {
    if (!requireLogin()) return
    if (!this.checkId) this.loadOrders()
  },
  methods: {
    typeText(t) { return ({ FULL: '全盘', PART: '抽盘', CYCLE: '循环盘' })[t] || t },
    statusText(s) { return ({ DRAFT: '盘点中', FINISHED: '已完成', CANCELLED: '已作废' })[s] || s },
    statusCls(s) { return ({ DRAFT: 'st-draft', FINISHED: 'st-done', CANCELLED: 'st-cancel' })[s] || '' },

    async loadOrders() {
      try {
        // GET /api/stock/check/orders
        const data = await request('/stock/check/orders', { params: { page: 1, size: 50, sort: 'createTime,desc' } })
        this.orders = recordsOf(data)
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
        this.orders = []
      }
    },

    async selectOrder(id) {
      this.checkId = id
      await this.loadDetail()
    },
    backToList() {
      this.checkId = null
      this.detail = {}
      this.items = []
      this.loadOrders()
    },

    async loadDetail() {
      try {
        // GET /api/stock/check/orders/{id}
        const d = await request('/stock/check/orders/' + this.checkId)
        this.detail = d || {}
        this.items = (d.items || []).map((it) => ({
          itemId: it.id,
          isbn: it.isbn,
          bookName: it.bookName,
          bookQty: Number(it.bookQty || 0),
          realQty: Number(it.realQty || 0)
        }))
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
      }
    },

    scan() {
      // #ifdef APP-PLUS || MP-WEIXIN
      uni.scanCode({
        scanType: ['barCode', 'qrCode'],
        success: (res) => { this.doScan(res.result) },
        fail: () => {}
      })
      // #endif
      // #ifndef APP-PLUS || MP-WEIXIN
      uni.showToast({ title: '请手动输入 ISBN', icon: 'none' })
      // #endif
    },
    scanManual() {
      if (!this.manualCode.trim()) { uni.showToast({ title: '请输入 ISBN', icon: 'none' }); return }
      this.doScan(this.manualCode.trim())
      this.manualCode = ''
    },

    async doScan(code) {
      try {
        // POST /api/mobile/check/scan  { checkId, code, realQty }
        await request('/mobile/check/scan', {
          method: 'POST',
          data: { checkId: Number(this.checkId), code, realQty: 1 }
        })
        uni.showToast({ title: '已录入', icon: 'success' })
        await this.loadDetail()
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
      }
    },

    async saveItems() {
      const payload = this.items.map((it) => ({ itemId: it.itemId, realQty: Number(it.realQty) }))
      try {
        // PUT /api/stock/check/orders/{id}/items
        await request('/stock/check/orders/' + this.checkId + '/items', { method: 'PUT', data: { items: payload } })
        uni.showToast({ title: '已保存', icon: 'success' })
        await this.loadDetail()
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
      }
    },

    finish() {
      uni.showModal({
        title: '完成盘点',
        content: '确认完成？系统将按差异调整库存并写入流水。',
        success: async (res) => {
          if (!res.confirm) return
          try {
            // POST /api/stock/check/orders/{id}/finish
            await request('/stock/check/orders/' + this.checkId + '/finish', { method: 'POST' })
            uni.showToast({ title: '盘点完成', icon: 'success' })
            this.backToList()
          } catch (e) {
            uni.showToast({ title: e.message, icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style>
.order-item { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.order-item:last-child { border-bottom: none; }
.oi-left { flex: 1; display: flex; flex-direction: column; }
.oi-no { font-size: 30rpx; font-weight: 600; }
.oi-sub { font-size: 22rpx; color: #8a93a6; margin-top: 6rpx; }
.oi-status { font-size: 24rpx; padding: 6rpx 18rpx; border-radius: 30rpx; }
.st-draft { background: #eef2fa; color: #5566aa; }
.st-done { background: #e6f6ee; color: #22a06b; }
.st-cancel { background: #fdecec; color: #e5484d; }
.scan-btn { height: 100rpx; line-height: 100rpx; background: #2f6fed; color: #fff; border-radius: 14rpx; font-size: 32rpx; }
.scan-btn::after { border: none; }
.manual { display: flex; }
.manual .input { flex: 1; height: 78rpx; border: 1rpx solid #e4e8f0; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.btn-add { margin-left: 12rpx; height: 78rpx; line-height: 78rpx; padding: 0 28rpx; background: #eef3ff; color: #2f6fed; border-radius: 12rpx; font-size: 28rpx; }
.btn-add::after { border: none; }
.item-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.item-row:last-child { border-bottom: none; }
.it-left { flex: 1; display: flex; flex-direction: column; }
.it-name { font-size: 28rpx; }
.it-isbn { font-size: 22rpx; color: #8a93a6; margin-top: 4rpx; }
.it-right { display: flex; align-items: center; }
.it-qty { font-size: 22rpx; color: #8a93a6; margin-right: 12rpx; }
.it-input { width: 110rpx; height: 60rpx; border: 1rpx solid #e4e8f0; border-radius: 10rpx; text-align: center; font-size: 28rpx; }
.it-diff { width: 70rpx; text-align: right; color: #8a93a6; font-size: 26rpx; }
.it-diff.diff { color: #e5484d; font-weight: 700; }
.footer { display: flex; margin-top: 24rpx; }
.footer button { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 14rpx; font-size: 30rpx; }
.btn-save { background: #fff; color: #2f6fed; border: 1rpx solid #2f6fed; margin-right: 20rpx; }
.btn-finish { background: #22a06b; color: #fff; }
.footer button::after { border: none; }
</style>