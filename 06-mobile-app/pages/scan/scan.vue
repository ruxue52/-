<template>
  <view class="container">
    <view class="card">
      <view class="card-title">扫码识别</view>
      <button class="scan-btn" @click="startScan">📷 开始扫码</button>
      <view class="muted mt8">支持图书 / 单据 / 库存位 / 盘点 二维码，内容规范见接口文档。</view>
      <view class="manual mt16">
        <input class="input" v-model="manual" placeholder="或手动输入 ISBN / 扫码内容" />
        <button class="btn" @click="resolve(manual)">解析</button>
      </view>
    </view>

    <view class="card" v-if="result">
      <view class="row">
        <view class="card-title" style="margin:0">解析结果</view>
        <view class="spacer"></view>
        <text class="tag">{{ result.bizType }}</text>
      </view>

      <view class="kv-list mt8">
        <view class="kv" v-for="(item, i) in entityRows" :key="i">
          <text class="k">{{ item.k }}</text>
          <text class="v">{{ item.v }}</text>
        </view>
      </view>

      <view class="actions mt16">
        <button class="btn primary" v-if="isbn" @click="queryStock">查看库存</button>
        <button class="btn" v-if="isbn" @click="viewQr">图书二维码</button>
      </view>
    </view>

    <view class="card" v-if="stockList.length">
      <view class="card-title">实时库存</view>
      <view class="stock-row" v-for="(s, i) in stockList" :key="i">
        <text class="s-wh">{{ s.warehouseName || s.warehouseId }}</text>
        <text class="s-qty" :class="{ low: isLow(s) }">{{ s.quantity }}</text>
        <text class="s-shelf">{{ s.shelfNo || '-' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request, requireLogin, extractIsbn, fmtMoney, BASE_URL } from '@/utils/request.js'

export default {
  data() {
    return {
      manual: '',
      rawCode: '',
      result: null,
      stockList: []
    }
  },
  computed: {
    isbn() {
      if (!this.result) return ''
      const e = this.result.entity || {}
      return e.isbn || extractIsbn(this.rawCode || '')
    },
    entityRows() {
      const e = (this.result && this.result.entity) || {}
      const rows = []
      const push = (k, v) => { if (v !== undefined && v !== null && v !== '') rows.push({ k, v }) }
      if (e.isbn) push('ISBN', e.isbn)
      if (e.bookName) push('书名', e.bookName)
      if (e.author) push('作者', e.author)
      if (e.publisherName) push('出版社', e.publisherName)
      if (e.categoryName) push('分类', e.categoryName)
      if (e.price !== undefined) push('定价', '¥' + fmtMoney(e.price))
      if (e.shelfNo) push('货架位', e.shelfNo)
      if (e.orderNo) push('单号', e.orderNo)
      if (e.status) push('状态', e.status)
      if (!rows.length) push('内容', this.rawCode || '-')
      return rows
    }
  },
  onShow() {
    requireLogin()
  },
  methods: {
    isLow(s) { return Number(s.quantity) < Number(s.safetyStock || 0) },
    startScan() {
      // #ifdef APP-PLUS || MP-WEIXIN
      uni.scanCode({
        scanType: ['barCode', 'qrCode'],
        success: (res) => { this.resolve(res.result) },
        fail: () => { uni.showToast({ title: '扫码取消', icon: 'none' }) }
      })
      // #endif
      // #ifndef APP-PLUS || MP-WEIXIN
      uni.showToast({ title: '当前平台请在下方手动输入', icon: 'none' })
      // #endif
    },
    async resolve(code) {
      code = (code || '').trim()
      if (!code) { uni.showToast({ title: '请输入内容', icon: 'none' }); return }
      this.rawCode = code
      this.stockList = []
      try {
        // POST /api/scan/resolve
        this.result = await request('/scan/resolve', { method: 'POST', data: { code } })
        // 记录扫码日志
        request('/scan/log', {
          method: 'POST',
          data: { code, bizType: this.result.bizType, bizId: (this.result.entity || {}).isbn || '', device: 'APP', result: 'SUCCESS' }
        }).catch(() => {})
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
        this.result = null
      }
    },
    async queryStock() {
      try {
        const quick = await request('/mobile/stock/quick', { params: { keyword: this.isbn } })
        this.stockList = (quick && (quick.stockList || quick.stocks)) || []
        if (!this.result) this.result = { bizType: 'BOOK', entity: quick && quick.book ? quick.book : quick }
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
      }
    },
    viewQr() {
      // #ifdef APP-PLUS || H5 || MP-WEIXIN
      const url = BASE_URL + '/qrcode/book/' + encodeURIComponent(this.isbn)
      uni.previewImage({ urls: [url] })
      // #endif
    }
  }
}
</script>

<style>
.scan-btn { height: 120rpx; line-height: 120rpx; background: #2f6fed; color: #fff; border-radius: 16rpx; font-size: 36rpx; }
.scan-btn::after { border: none; }
.manual { display: flex; }
.manual .input { flex: 1; height: 80rpx; border: 1rpx solid #e4e8f0; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.manual .btn { margin-left: 16rpx; height: 80rpx; line-height: 80rpx; background: #eef3ff; color: #2f6fed; border-radius: 12rpx; font-size: 28rpx; }
.manual .btn::after { border: none; }
.tag { background: #eef3ff; color: #2f6fed; font-size: 22rpx; padding: 6rpx 18rpx; border-radius: 30rpx; }
.kv { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.kv:last-child { border-bottom: none; }
.kv .k { width: 150rpx; color: #6b7688; }
.kv .v { flex: 1; color: #1f2733; }
.actions { display: flex; }
.actions .btn { flex: 1; text-align: center; height: 80rpx; line-height: 80rpx; border-radius: 12rpx; font-size: 28rpx; margin-right: 16rpx; background: #eef3ff; color: #2f6fed; }
.actions .btn.primary { background: #2f6fed; color: #fff; margin-right: 0; }
.actions .btn::after { border: none; }
.stock-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.stock-row:last-child { border-bottom: none; }
.s-wh { flex: 1; }
.s-qty { width: 160rpx; text-align: center; font-weight: 700; color: #22a06b; }
.s-qty.low { color: #e5484d; }
.s-shelf { width: 160rpx; text-align: right; color: #6b7688; }
</style>