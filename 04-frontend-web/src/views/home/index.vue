<template>
  <div class="home">
    <div class="home-bg" :style="{ backgroundImage: `url(${bgImg})` }"></div>
    <div class="home-veil"></div>

    <!-- 漂浮诗词 -->
    <div class="poem-field" aria-hidden="true">
      <span v-for="(p, i) in poems" :key="i" class="poem-line" :style="lineStyle(p)">{{ p.text }}</span>
    </div>

    <!-- 右上角登录 -->
    <header class="home-top">
      <div class="brand-mini">
        <el-icon class="mini-icon"><Reading /></el-icon>
        <span>晓镜书阁</span>
      </div>
      <div class="home-actions">
        <button class="text-action" @click="goLogin">登录</button>
      </div>
    </header>

    <main class="home-main">
      <h1 class="brand-title">晓镜书阁</h1>
      <p class="brand-sub">书店进销存管理系统</p>
      <div class="rule"></div>
      <p class="brand-motto">以书为镜 · 可知兴替</p>
    </main>

    <footer class="home-foot">晓镜书阁 · 书店进销存管理系统</footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import bgImg from '@/assets/book-bg.jpg'

const router = useRouter()

// 漂浮的诗词（围绕「书」与『晓镜』，对应品牌名）
const poems = [
  { text: '晓镜但愁云鬓改，夜吟应觉月光寒', x: 6, y: 16, size: 20, dur: 28, delay: 0, op: 0.72 },
  { text: '书卷多情似故人，晨昏忧乐每相亲', x: 54, y: 10, size: 17, dur: 34, delay: 5, op: 0.6 },
  { text: '问渠那得清如许？为有源头活水来', x: 60, y: 30, size: 18, dur: 26, delay: 11, op: 0.64 },
  { text: '旧书不厌百回读，熟读深思子自知', x: 5, y: 42, size: 19, dur: 32, delay: 3, op: 0.66 },
  { text: '半亩方塘一鉴开，天光云影共徘徊', x: 10, y: 72, size: 18, dur: 30, delay: 14, op: 0.62 },
  { text: '纸上得来终觉浅，绝知此事要躬行', x: 52, y: 64, size: 18, dur: 36, delay: 8, op: 0.6 },
  { text: '读书破万卷，下笔如有神', x: 34, y: 84, size: 16, dur: 24, delay: 18, op: 0.58 },
  { text: '眼前直下三千字，胸次全无一点尘', x: 58, y: 82, size: 17, dur: 33, delay: 2, op: 0.56 }
]

function lineStyle(p) {
  return {
    left: p.x + '%',
    top: p.y + '%',
    fontSize: p.size + 'px',
    animationDuration: p.dur + 's',
    animationDelay: p.delay + 's',
    '--max-op': p.op
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.home {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background: #eceae4;
  font-family: 'Songti SC', 'STSong', 'SimSun', 'Noto Serif SC', serif;
  user-select: none;
}

/* 背景图（摊开的诗词集） */
.home-bg {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  transform: scale(1.02);
}

/* 中间留白、四周压暗，兼顾画面与文字可读性 */
.home-veil {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse 64% 72% at 50% 50%,
    rgba(252, 250, 246, 0.92) 0%,
    rgba(252, 250, 246, 0.56) 48%,
    rgba(20, 28, 24, 0.42) 100%
  );
}

/* 漂浮诗词层 */
.poem-field {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
  pointer-events: none;
}

.poem-line {
  position: absolute;
  white-space: nowrap;
  color: #55605a;
  letter-spacing: 3px;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.85);
  opacity: 0;
  will-change: transform, opacity;
  animation-name: drift;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

@keyframes drift {
  0% {
    transform: translate3d(0, 26px, 0);
    opacity: 0;
  }
  14% {
    opacity: var(--max-op, 0.6);
  }
  50% {
    transform: translate3d(16px, -10px, 0);
  }
  86% {
    opacity: var(--max-op, 0.6);
  }
  100% {
    transform: translate3d(-8px, -46px, 0);
    opacity: 0;
  }
}

/* 右上角 */
.home-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 32px;
}

.brand-mini {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  letter-spacing: 3px;
  color: #fff;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.45);
}

.mini-icon {
  font-size: 18px;
  color: #fff;
}

.home-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* 纯文字入口：无按钮外形、无边框，仅悬停时出现下划线 */
.text-action {
  padding: 0 0 2px;
  border: none;
  border-bottom: 1px solid transparent;
  background: transparent;
  font-family: inherit;
  font-size: 16px;
  letter-spacing: 4px;
  color: #fff;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.45);
  cursor: pointer;
  transition: border-color 0.2s ease, opacity 0.2s ease;
}

.text-action:hover {
  border-bottom-color: rgba(255, 255, 255, 0.75);
  opacity: 0.9;
}

/* 中央品牌 */
.home-main {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  pointer-events: none;
}

.brand-title {
  margin: 0;
  font-size: clamp(40px, 8vw, 72px);
  font-weight: 500;
  letter-spacing: 16px;
  text-indent: 16px;
  color: #23302a;
}

.brand-sub {
  margin: 14px 0 0;
  font-size: 15px;
  letter-spacing: 6px;
  color: #5c6862;
}

.rule {
  width: 56px;
  height: 1px;
  margin: 26px 0;
  background: linear-gradient(90deg, transparent, #a9b2ab, transparent);
}

.brand-motto {
  margin: 0;
  font-size: 14px;
  letter-spacing: 5px;
  color: #6d7a72;
}

.home-foot {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  z-index: 3;
  text-align: center;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.45);
}
</style>
