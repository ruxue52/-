<template>
  <view class="login-page">
    <view class="login-head">
      <view class="logo">书</view>
      <view class="app-name">晓镜书阁 · 进销存</view>
      <view class="app-sub">移动端（Android / iOS / 鸿蒙）</view>
    </view>

    <view class="login-form">
      <view class="field">
        <text class="label">账号</text>
        <input class="input" v-model="username" placeholder="请输入账号，如 admin" />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input class="input" v-model="password" password placeholder="请输入密码" />
      </view>

      <view class="msg">{{ msg }}</view>
      <button class="btn" :loading="loading" @click="doLogin">登 录</button>
      <view class="tip">默认账号：admin / 123456</view>
    </view>
  </view>
</template>

<script>
import { request, setAuth, BASE_URL } from '@/utils/request.js'

export default {
  data() {
    return {
      username: '',
      password: '',
      msg: '',
      loading: false
    }
  },
  onLoad() {
    // 已登录则直接进入首页
    if (uni.getStorageSync('shg_token')) {
      uni.reLaunch({ url: '/pages/index/index' })
    }
  },
  methods: {
    async doLogin() {
      if (!this.username.trim() || !this.password) {
        this.msg = '请输入账号与密码'
        return
      }
      this.loading = true
      this.msg = ''
      try {
        // POST /api/auth/login
        const data = await request('/auth/login', {
          method: 'POST',
          data: { username: this.username.trim(), password: this.password }
        })
        setAuth(data.token, data.userInfo)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 500)
      } catch (e) {
        this.msg = e.message || '登录失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style>
.login-page { min-height: 100vh; background: linear-gradient(160deg, #3b78f0, #2f6fed 45%, #2456c9); padding: 120rpx 48rpx 0; box-sizing: border-box; }
.login-head { text-align: center; color: #fff; margin-bottom: 60rpx; }
.login-head .logo { width: 120rpx; height: 120rpx; line-height: 120rpx; border-radius: 28rpx; background: rgba(255,255,255,0.2); font-size: 60rpx; font-weight: 700; margin: 0 auto 24rpx; }
.login-head .app-name { font-size: 44rpx; font-weight: 700; }
.login-head .app-sub { font-size: 24rpx; opacity: 0.85; margin-top: 8rpx; }
.login-form { background: #fff; border-radius: 24rpx; padding: 48rpx 36rpx; box-shadow: 0 12rpx 40rpx rgba(0,0,0,0.12); }
.field { margin-bottom: 32rpx; }
.field .label { font-size: 26rpx; color: #6b7688; display: block; margin-bottom: 12rpx; }
.field .input { height: 88rpx; border: 1rpx solid #e4e8f0; border-radius: 12rpx; padding: 0 24rpx; font-size: 30rpx; }
.msg { color: #e5484d; font-size: 26rpx; min-height: 40rpx; }
.btn { height: 92rpx; line-height: 92rpx; background: #2f6fed; color: #fff; border-radius: 12rpx; font-size: 32rpx; margin-top: 8rpx; }
.btn::after { border: none; }
.tip { text-align: center; color: #8a93a6; font-size: 24rpx; margin-top: 24rpx; }
</style>