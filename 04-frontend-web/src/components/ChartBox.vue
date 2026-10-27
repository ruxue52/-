<template>
  <div ref="chartRef" class="chart-box" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: [Number, String], default: 320 }
})

const chartRef = ref(null)
let chart = null

function render() {
  if (!chart && chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  if (chart && props.option) {
    chart.setOption(props.option, true)
  }
}

function resize() {
  if (chart) chart.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(
  () => props.option,
  () => {
    nextTick(render)
  },
  { deep: true }
)

defineExpose({ resize })
</script>