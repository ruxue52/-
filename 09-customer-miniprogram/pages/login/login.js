// 读者端 · 会员登录（手机号 + 短信验证码）
const api = require('../../utils/request')

Page({
  data: {
    phone: '',
    code: '',
    counting: 0,
    sending: false,
    logging: false,
    hint: '',
    demoAccounts: ['13900000001', '13900000002', '13900000003', '13900000004']
  },

  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onCodeInput(e) {
    this.setData({ code: e.detail.value })
  },

  sendCode() {
    const phone = (this.data.phone || '').trim()
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的 11 位手机号', icon: 'none' })
      return
    }
    if (this.data.counting > 0 || this.data.sending) return
    this.setData({ sending: true })
    api.request('/reader/auth/send-code', { method: 'POST', data: { phone } })
      .then((data) => {
        this.startCountdown()
        if (data && data.code) {
          // 后端未接短信网关，演示环境直接回显验证码
          this.setData({ code: data.code, hint: '演示环境验证码：' + data.code + '（有效期 ' + data.expireMinutes + ' 分钟）' })
          wx.showToast({ title: '验证码：' + data.code, icon: 'none', duration: 3000 })
        } else {
          this.setData({ hint: '验证码已发送，请查收短信' })
        }
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      .then(() => this.setData({ sending: false }))
  },

  startCountdown() {
    this.setData({ counting: 60 })
    if (this.timer) clearInterval(this.timer)
    this.timer = setInterval(() => {
      const next = this.data.counting - 1
      this.setData({ counting: next })
      if (next <= 0) clearInterval(this.timer)
    }, 1000)
  },

  onLogin() {
    const phone = (this.data.phone || '').trim()
    const code = (this.data.code || '').trim()
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!code) {
      wx.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
    if (this.data.logging) return
    this.setData({ logging: true })
    api.request('/reader/auth/login', { method: 'POST', data: { phone, code } })
      .then((data) => {
        getApp().setAuth(data.token, data.member)
        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack({
            fail: () => wx.switchTab({ url: '/pages/member/member' })
          })
        }, 800)
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      .then(() => this.setData({ logging: false }))
  },

  useDemo(e) {
    this.setData({ phone: e.currentTarget.dataset.phone, code: '' })
  }
})
