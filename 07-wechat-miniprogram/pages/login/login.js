// 晓镜书阁 · 登录页
const { request } = require('../../utils/request.js')
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    msg: '',
    loading: false
  },

  onLoad() {
    // 已登录直接进入首页
    if (app.getToken()) {
      wx.reLaunch({ url: '/pages/index/index' })
    }
  },

  onUsername(e) { this.setData({ username: e.detail.value }) },
  onPassword(e) { this.setData({ password: e.detail.value }) },

  doLogin() {
    const username = (this.data.username || '').trim()
    const password = this.data.password || ''
    if (!username || !password) {
      this.setData({ msg: '请输入账号与密码' })
      return
    }
    this.setData({ loading: true, msg: '' })
    // POST /api/auth/login
    request('/auth/login', { method: 'POST', data: { username, password } })
      .then((data) => {
        app.setAuth(data.token, data.userInfo)
        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 500)
      })
      .catch((e) => {
        this.setData({ msg: e.message || '登录失败' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  }
})