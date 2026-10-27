<template>
  <div class="login-page">
    <div class="login-bg" :style="{ backgroundImage: `url(${bgImg})` }"></div>
    <div class="login-veil"></div>

    <div class="login-inner">
      <div class="login-brand">
        <el-icon class="brand-icon"><Reading /></el-icon>
        <h1 class="brand-title">晓镜书阁</h1>
        <p class="brand-sub">书店进销存管理系统</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="工号 / 用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="remember">记住账号</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">默认账号：<b>admin</b> / <b>123456</b></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import bgImg from '@/assets/book-bg.jpg'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  username: localStorage.getItem('shg_remember_user') || 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入工号或用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    if (remember.value) {
      localStorage.setItem('shg_remember_user', form.username)
    } else {
      localStorage.removeItem('shg_remember_user')
    }
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #eceae4;
  font-family: 'Songti SC', 'STSong', 'SimSun', 'Noto Serif SC', serif;
}

/* 背景图（摊开的诗词集） */
.login-bg {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  transform: scale(1.02);
}

/* 中间留白、四周压暗：表单直接浮在画面上，不设卡片 */
.login-veil {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse 58% 66% at 50% 50%,
    rgba(252, 250, 246, 0.94) 0%,
    rgba(252, 250, 246, 0.6) 50%,
    rgba(20, 28, 24, 0.46) 100%
  );
}

.login-inner {
  position: relative;
  z-index: 2;
  width: min(90vw, 380px);
  text-align: center;
}

.login-brand {
  margin-bottom: 30px;
}

.brand-icon {
  font-size: 34px;
  color: #3d4a42;
}

.brand-title {
  margin: 10px 0 6px;
  font-size: 32px;
  font-weight: 500;
  letter-spacing: 8px;
  text-indent: 8px;
  color: #23302a;
}

.brand-sub {
  margin: 0;
  font-size: 13px;
  letter-spacing: 4px;
  color: #5c6862;
}

/* 表单不加任何容器边框，用柔和投影与画面分离 */
.login-form {
  filter: drop-shadow(0 8px 22px rgba(24, 32, 28, 0.14));
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-checkbox__label) {
  color: #4c5a53;
}

.login-btn {
  width: 100%;
  letter-spacing: 6px;
  font-weight: 600;
}

.login-tip {
  font-size: 12px;
  color: #5f6c65;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}
</style>
