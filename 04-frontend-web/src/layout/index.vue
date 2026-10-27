<template>
  <el-container class="layout-root">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <el-icon class="logo-icon"><Reading /></el-icon>
        <span v-show="!collapsed" class="logo-text">晓镜书阁</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#1f2d3d"
          text-color="#c0c4cc"
          active-text-color="#ffd04b"
          router
          unique-opened
        >
          <template v-for="item in visibleMenus" :key="item.path">
            <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="child in visibleChildren(item)"
                :key="child.path"
                :index="child.path"
              >
                <el-icon><component :is="child.icon" /></el-icon>
                <template #title>{{ child.title }}</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(bc, i) in breadcrumbs" :key="i">{{ bc }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="plain" size="small">{{ today }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="10">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 个人信息抽屉 -->
    <el-drawer v-model="profileVisible" title="个人信息" size="360px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">{{ userStore.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userStore.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-for="r in userStore.roles" :key="r" size="small" class="mr-4">{{ r }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="权限数">{{ userStore.permissions.length }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { menuList } from '@/router/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const profileVisible = ref(false)

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const items = []
  route.matched.forEach((m) => {
    if (m.meta && m.meta.title) items.push(m.meta.title)
  })
  return items
})

const avatarText = computed(() => (userStore.realName || 'U').slice(0, 1))

const today = computed(() => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
})

// 按权限过滤后的菜单
const visibleMenus = computed(() => {
  return menuList
    .map((m) => {
      const children = (m.children || []).filter((c) => userStore.hasPerm(c.perm))
      return { ...m, children }
    })
    .filter((m) => {
      // 有子菜单的：需自身可见且有可见子项
      if (m.children && m.children.length) {
        return userStore.hasPerm(m.perm) && m.children.length > 0
      }
      // 原本有 children 定义但被过滤空了 -> 整体隐藏
      const original = menuList.find((x) => x.path === m.path)
      if (original && original.children && original.children.length) return false
      return userStore.hasPerm(m.perm)
    })
})

function visibleChildren(item) {
  return item.children || []
}

function onCommand(cmd) {
  if (cmd === 'profile') {
    profileVisible.value = true
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.replace('/')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout-root {
  height: 100vh;
}

.layout-aside {
  background-color: #1f2d3d;
  transition: width 0.25s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background-color: #17212e;
  gap: 8px;
  overflow: hidden;
}

.logo-icon {
  font-size: 24px;
  color: #ffd04b;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}

.layout-aside :deep(.el-menu) {
  border-right: none;
}

.layout-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: #409eff;
  color: #fff;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.layout-main {
  background: #f0f2f5;
  padding: 0;
  overflow-y: auto;
}

.mr-4 {
  margin-right: 4px;
}
</style>