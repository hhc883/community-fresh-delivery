<template>
  <div class="report-trend">
    <div class="trend-legend" aria-label="图例">
      <span v-for="item in series" :key="item.name"><i :style="{ background: item.color }"></i>{{ item.name }}</span>
      <span class="trend-unit">单位：{{ unit }}</span>
    </div>
    <svg v-if="dates.length" class="trend-svg" viewBox="0 0 800 220" role="img" :aria-label="`${title}，共 ${dates.length} 天；可展开明细查看准确数值`">
      <g v-for="tick in yTicks" :key="tick.y">
        <line x1="60" :y1="tick.y" x2="778" :y2="tick.y" stroke="#e7eee9" stroke-dasharray="4 4" />
        <text x="50" :y="tick.y + 4" text-anchor="end" class="axis-label">{{ tick.label }}</text>
      </g>
      <g v-for="item in plots" :key="item.name">
        <polyline :points="item.path" fill="none" :stroke="item.color" stroke-width="2.5" stroke-linejoin="round" />
        <circle v-for="point in item.points" :key="point.index" :cx="point.x" :cy="point.y" r="3" :fill="item.color" stroke="white" stroke-width="1">
          <title>{{ point.date }} · {{ item.name }}：{{ point.value }} {{ unit }}</title>
        </circle>
      </g>
      <text v-for="tick in xTicks" :key="tick.index" :x="tick.x" y="207" :text-anchor="tick.anchor" class="axis-label">{{ tick.date }}</text>
    </svg>
    <div v-else class="chart-empty">所选区间暂无数据</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  dates: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
  unit: { type: String, default: '元' }
})
const ceiling = computed(() => {
  const values = props.series.flatMap(item => item.values).map(Number).filter(Number.isFinite)
  const max = Math.max(0, ...values)
  if (props.unit !== '元') return Math.max(2, Math.ceil(max / 2) * 2)
  if (!max) return 1
  const step = 10 ** Math.floor(Math.log10(max))
  return Math.ceil(max / step) * step
})
const xAt = index => props.dates.length === 1 ? 419 : 60 + index / (props.dates.length - 1) * 718
const yTicks = computed(() => [0, 0.5, 1].map(ratio => ({
  y: 178 - ratio * 154,
  label: (ceiling.value * ratio).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
})))
const xTicks = computed(() => [...new Set([0, Math.floor((props.dates.length - 1) / 2), props.dates.length - 1])]
  .filter(index => index >= 0 && props.dates[index])
  .map(index => ({ index, x: xAt(index), date: props.dates[index], anchor: props.dates.length === 1 ? 'middle' : index === 0 ? 'start' : index === props.dates.length - 1 ? 'end' : 'middle' })))
const plots = computed(() => props.series.map(item => {
  const points = props.dates.flatMap((date, index) => {
    const value = item.values[index]
    if (value === null || value === undefined || !Number.isFinite(Number(value))) return []
    return [{ index, date, value, x: xAt(index), y: 178 - Number(value) / ceiling.value * 154 }]
  })
  return { ...item, points, path: points.map(point => `${point.x},${point.y}`).join(' ') }
}))
</script>

<style scoped>
.trend-legend { display: flex; align-items: center; gap: 20px; color: #586b60; font-size: 12px; }
.trend-legend span { display: inline-flex; align-items: center; gap: 7px; }
.trend-legend i { width: 16px; height: 3px; border-radius: 2px; }
.trend-unit { margin-left: auto; }
.trend-svg { display: block; width: 100%; height: 210px; }
.axis-label { fill: #708277; font-size: 12px; font-family: inherit; }
.chart-empty { min-height: 210px; display: grid; place-items: center; color: #708277; font-size: 13px; }
@media (max-height: 650px) { .trend-svg, .chart-empty { height: 180px; min-height: 180px; } }
</style>
