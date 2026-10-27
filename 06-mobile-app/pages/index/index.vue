<template>
  <view class="container">
    <view class="banner">
      <view class="banner-title">晓镜书阁 · 移动工作台</view>
      <view class="banner-sub">{{ userText }}</view>
      <view class="banner-stat" v-if="overview">
        <view class="bs-item">
          <text class="bs-num">{{ overview.todaySaleAmount !== undefined ? '¥' + fmtMoney(overview.todaySaleAmount) : '-' }}</text>
          <text class="bs-label">今日销售</text>
        </view>
        <view class="bs-item">
          <text class="bs-num">{{ overview.warningCount !== undefined ? overview.warningCount : '-' }}</text>
          <text class="bs-label">库存预警</text>
        </view>
        <view class="bs-item">
          <text class="bs-num">{{ overview.pendingCount !== undefined ? overview.pendingCount : '-' }}</text>
          <text class="bs-label">待处理</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">常用功能</view>
      <view class="grid">
        <view class="grid-item" @click="go('/pages/scan/scan', true)">
          <text class="gi-icon">📷</text>
          <text class="gi-text">扫码</text>
        </view>
        <view class="grid-item" @click="go('/pages/stock/stock', true)">
          <text class="gi-icon">📦</text>
          <text class="gi-text">库存查询</text>
        </view>
        <view class="grid-item" @click="go('/pages/check/check')">
          <text class="gi-icon">🧾</text>
          <text class="gi-text">移动盘点</text>
        </view>
        <view class="grid-item" @click="go('/pages/order/order', true)">
          <text class="gi-icon">📄</text>
          <text class="gi-text">单据查询</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">移动端统计</view>
      <view class="stat-list">
        <view class="stat-row" v-for="(item, i) in statList" :key="i">
          <text class="s-label">{{ item.label }}</text>
          <text class="s-value">{{ item.value }}</text>
        </view>
        <view v-if="!statList.length" class="muted center">暂无统计数据（请确认后端已启动）</view>
      </view>
    </view>

    <button class="logout" @click="logout">退出登录</button>
  </view>
</template>

<script>
import { request, requireLogin, getUser, clearAuth, fmtMoney } from '@/utils/request.js'

export default {
  data() {
    return {
      overview: null,
      statList: []
    }
  },
  computed: {
    userText() {
      const u = getUser()
      return u && (u.realName || u.username) ? '您好，' + (u.realName || u.username) : '您好'
    }
  },
  onShow() {
    if (!requireLogin()) return
    this.loadOverview()
    this.loadStat()
  },
  methods: {
    fmtMoney,
    go(url, isTab) {
      if (isTab) uni.switchTab({ url })
      else uni.navigateTo({ url })
    },
    async loadOverview() {
      try {
        // GET /api/mobile/overview
        this.overview = await request('/mobile/overview')
      } catch (e) {
        this.overview = null
      }
    },
    async loadStat() {
      try {
        // GET /api/mobile/stat/summary
        const data = await request('/mobile/stat/summary')
        const LABELS = {
          todaySaleAmount: '今日销售额',
          todaySaleCount: '今日销售单数',
          monthSaleAmount: '本月销售额',
          stockAmount: '库存总额',
          warningCount: '库存预警数',
          bookCount: '图书品种数',
          totalSku: 'SKU 数'
        }
        const list = []
        Object.keys(data || {}).forEach((k) => {
          let v = data[k]
          if (typeof v === 'number' && /amount/i.test(k)) v = '¥' + fmtMoney(v)
          list.push({ label: LABELS[k] || k, value: v })
        })
        this.statList = list
      } catch (e) {
        this.statList = []
      }
    },
    logout() {
      uni.showModal({
        title: '提示',
        content: '确认退出登录？',
        success: (res) => {
          if (res.confirm) {
            request('/auth/logout', { method: 'POST' }).catch(() => {})
            clearAuth()
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    }
  }
}
</script>

<style>
.banner { background: linear-gradient(135deg, #3b78f0, #2f6fed); border-radius: 16rpx; padding: 32rpx; color: #fff; margin-bottom: 24rpx; }
.banner-title { font-size: 34rpx; font-weight: 700; }
.banner-sub { font-size: 24rpx; opacity: 0.85; margin-top: 8rpx; }
.banner-stat { display: flex; margin-top: 28rpx; }
.bs-item { flex: 1; display: flex; flex-direction: column; align-items: center; }
.bs-num { font-size: 34rpx; font-weight: 700; }
.bs-label { font-size: 22rpx; opacity: 0.85; margin-top: 6rpx; }
.grid { display: flex; flex-wrap: wrap; }
.grid-item { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 20rpx 0; }
.gi-icon { font-size: 52rpx; }
.gi-text { font-size: 24rpx; color: #4a5568; margin-top: 10rpx; }
.stat-row { display: flex; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f7; }
.stat-row:last-child { border-bottom: none; }
.s-label { color: #6b7688; }
.s-value { font-weight: 600; color: #2f6fed; }
.logout { margin-top: 24rpx; background: #fff; color: #e5484d; border-radius: 12rpx; font-size: 30rpx; }
.logout::after { border: none; }
</style>