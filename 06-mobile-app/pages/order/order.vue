<template>
  <view class="container">
    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'sale' }" @click="switchTab('sale')">出货单</view>
      <view class="tab" :class="{ active: tab === 'purchase' }" @click="switchTab('purchase')">进货单</view>
    </view>

    <view class="card" v-for="o in list" :key="o.id" @click="openDetail(o)">
      <view class="row">
        <text class="o-no">{{ o.orderNo }}</text>
        <view class="spacer"></view>
        <text class="tag" :class="statusCls(o.status)">{{ statusText(o.status) }}</text>
      </view>
      <view class="o-line">
        <text class="o-sub">{{ o.warehouseName || ('仓库' + o.warehouseId) }}</text>
        <text class="o-amount">¥{{ fmtMoney(tab === 'sale' ? o.payAmount : o.payAmount) }}</text>
      </view>
      <view class="o-sub">{{ fmtTime(o.createTime) }}　|　{{ tab === 'sale' ? (o.customerName || '散户') : (o.supplierName || '供应商') }}</view>
    </view>

    <view v-if="!list.length" class="card muted center">暂无单据数据</view>

    <!-- 单据详情弹层 -->
    <view class="mask" v-if="detail" @click="detail = null">
      <view class="detail-box" @click.stop>
        <view class="detail-title">{{ detail.orderNo }}</view>
        <view class="kv"><text class="k">仓库</text><text class="v">{{ detail.warehouseName || detail.warehouseId }}</text></view>
        <view class="kv"><text class="k">数量</text><text class="v">{{ detail.totalQty }}</text></view>
        <view class="kv"><text class="k">总金额</text><text class="v">¥{{ fmtMoney(detail.totalAmount) }}</text></view>
        <view class="kv"><text class="k">实付/应收</text><text class="v">¥{{ fmtMoney(detail.payAmount) }}</text></view>
        <view class="kv"><text class="k">经办人</text><text class="v">{{ detail.operatorName || '-' }}</text></view>
        <view class="kv"><text class="k">时间</text><text class="v">{{ fmtTime(detail.createTime) }}</text></view>
        <view class="card-title mt16">明细</view>
        <view class="di-row" v-for="(it, i) in (detail.items || [])" :key="i">
          <text class="di-name">{{ it.bookName }}</text>
          <text class="di-qty">×{{ it.quantity }}</text>
          <text class="di-amt">¥{{ fmtMoney(it.amount) }}</text>
        </view>
        <button class="close-btn" @click="detail = null">关闭</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request, requireLogin, recordsOf, fmtMoney, fmtTime } from '@/utils/request.js'

export default {
  data() {
    return {
      tab: 'sale',
      list: [],
      detail: null
    }
  },
  onShow() {
    if (!requireLogin()) return
    this.load()
  },
  methods: {
    fmtMoney, fmtTime,
    statusText(s) {
      return ({ DRAFT: '草稿', SUBMITTED: '已提交', INSTOCK: '已入库', OUTSTOCK: '已出库', CANCELLED: '已作废' })[s] || s
    },
    statusCls(s) {
      return ({ DRAFT: 'st-draft', SUBMITTED: 'st-warn', INSTOCK: 'st-done', OUTSTOCK: 'st-done', CANCELLED: 'st-cancel' })[s] || ''
    },
    switchTab(t) {
      this.tab = t
      this.load()
    },
    async load() {
      const path = this.tab === 'sale' ? '/sale/orders' : '/purchase/orders'
      try {
        const data = await request(path, { params: { page: 1, size: 30, sort: 'createTime,desc' } })
        this.list = recordsOf(data)
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
        this.list = []
      }
    },
    async openDetail(o) {
      try {
        const path = this.tab === 'sale' ? '/sale/orders/' : '/purchase/orders/'
        this.detail = await request(path + o.id)
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
      }
    }
  }
}
</script>

<style>
.tabs { display: flex; background: #fff; border-radius: 12rpx; padding: 8rpx; margin-bottom: 24rpx; }
.tab { flex: 1; text-align: center; padding: 18rpx 0; border-radius: 10rpx; color: #6b7688; font-size: 28rpx; }
.tab.active { background: #2f6fed; color: #fff; }
.o-no { font-size: 30rpx; font-weight: 600; }
.o-line { display: flex; align-items: center; margin-top: 12rpx; }
.o-sub { font-size: 22rpx; color: #8a93a6; flex: 1; }
.o-amount { font-size: 32rpx; font-weight: 700; color: #2f6fed; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 30rpx; }
.st-draft { background: #eef2fa; color: #5566aa; }
.st-warn { background: #fdf3e2; color: #e08a00; }
.st-done { background: #e6f6ee; color: #22a06b; }
.st-cancel { background: #fdecec; color: #e5484d; }
.mask { position: fixed; left: 0; top: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.45); display: flex; align-items: flex-end; z-index: 99; }
.detail-box { width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0; padding: 32rpx; max-height: 80vh; overflow-y: auto; box-sizing: border-box; }
.detail-title { font-size: 34rpx; font-weight: 700; margin-bottom: 16rpx; }
.kv { display: flex; padding: 12rpx 0; }
.kv .k { width: 180rpx; color: #6b7688; }
.kv .v { flex: 1; }
.di-row { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.di-name { flex: 1; }
.di-qty { width: 120rpx; text-align: center; }
.di-amt { width: 160rpx; text-align: right; color: #2f6fed; }
.close-btn { margin-top: 24rpx; height: 84rpx; line-height: 84rpx; background: #f2f4f8; color: #1f2733; border-radius: 12rpx; font-size: 30rpx; }
.close-btn::after { border: none; }
</style>