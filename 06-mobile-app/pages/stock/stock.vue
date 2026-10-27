<template>
  <view class="container">
    <view class="card">
      <view class="card-title">库存在线查询</view>
      <view class="search">
        <input class="input" v-model="keyword" placeholder="扫码或输入 ISBN / 书名" confirm-type="search" @confirm="search" />
        <button class="btn-scan" @click="scan">📷</button>
        <button class="btn-go" @click="search">查询</button>
      </view>
    </view>

    <view class="card" v-if="book">
      <view class="card-title">{{ book.bookName || '图书信息' }}</view>
      <view class="kv"><text class="k">ISBN</text><text class="v">{{ book.isbn || '-' }}</text></view>
      <view class="kv"><text class="k">作者</text><text class="v">{{ book.author || '-' }}</text></view>
      <view class="kv"><text class="k">出版社</text><text class="v">{{ book.publisherName || '-' }}</text></view>
      <view class="kv"><text class="k">定价</text><text class="v">¥{{ fmtMoney(book.price) }}</text></view>
      <view class="kv"><text class="k">货架位</text><text class="v">{{ book.shelfNo || '-' }}</text></view>
    </view>

    <view class="card" v-if="stockList.length">
      <view class="card-title">各仓库存</view>
      <view class="stock-row" v-for="(s, i) in stockList" :key="i">
        <view class="st-left">
          <text class="st-wh">{{ s.warehouseName || ('仓库' + s.warehouseId) }}</text>
          <text class="st-shelf">货架：{{ s.shelfNo || '-' }}　安全库存：{{ s.safetyStock || 0 }}</text>
        </view>
        <text class="st-qty" :class="{ low: isLow(s) }">{{ s.quantity }}</text>
      </view>
    </view>

    <view class="card center muted" v-else-if="searched && !stockList.length">
      未查询到该图书的库存记录
    </view>

    <view class="card" v-if="!book && !searched">
      <view class="muted">输入 ISBN 或使用扫码按钮，查询图书与各仓实时库存、货架位。</view>
    </view>
  </view>
</template>

<script>
import { request, requireLogin, extractIsbn, fmtMoney } from '@/utils/request.js'

export default {
  data() {
    return {
      keyword: '',
      book: null,
      stockList: [],
      searched: false
    }
  },
  onShow() {
    requireLogin()
  },
  methods: {
    fmtMoney,
    isLow(s) { return Number(s.quantity) < Number(s.safetyStock || 0) },
    scan() {
      // #ifdef APP-PLUS || MP-WEIXIN
      uni.scanCode({
        scanType: ['barCode', 'qrCode'],
        success: (res) => { this.keyword = extractIsbn(res.result); this.search() },
        fail: () => {}
      })
      // #endif
      // #ifndef APP-PLUS || MP-WEIXIN
      uni.showToast({ title: '请手动输入 ISBN', icon: 'none' })
      // #endif
    },
    async search() {
      const kw = (this.keyword || '').trim()
      if (!kw) { uni.showToast({ title: '请输入关键词', icon: 'none' }); return }
      this.searched = true
      try {
        // GET /api/mobile/stock/quick?keyword=
        const quick = await request('/mobile/stock/quick', { params: { keyword: kw } })
        if (quick && quick.book) {
          this.book = quick.book
          this.stockList = quick.stockList || quick.stocks || []
        } else if (quick && (quick.bookName || quick.isbn)) {
          this.book = quick
          this.stockList = quick.stockList || []
        } else {
          this.book = null
          this.stockList = []
        }
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
        this.book = null
        this.stockList = []
      }
    }
  }
}
</script>

<style>
.search { display: flex; align-items: center; }
.search .input { flex: 1; height: 84rpx; border: 1rpx solid #e4e8f0; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.btn-scan, .btn-go { height: 84rpx; line-height: 84rpx; padding: 0 24rpx; margin-left: 12rpx; border-radius: 12rpx; font-size: 28rpx; }
.btn-scan { background: #eef3ff; color: #2f6fed; }
.btn-go { background: #2f6fed; color: #fff; }
.btn-scan::after, .btn-go::after { border: none; }
.kv { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.kv:last-child { border-bottom: none; }
.kv .k { width: 150rpx; color: #6b7688; }
.kv .v { flex: 1; }
.stock-row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.stock-row:last-child { border-bottom: none; }
.st-left { flex: 1; display: flex; flex-direction: column; }
.st-wh { font-size: 30rpx; font-weight: 600; }
.st-shelf { font-size: 22rpx; color: #8a93a6; margin-top: 6rpx; }
.st-qty { font-size: 40rpx; font-weight: 700; color: #22a06b; }
.st-qty.low { color: #e5484d; }
</style>