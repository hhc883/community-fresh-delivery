<template>
  <div class="reports-page">
    <header class="report-heading">
      <div><h2>经营统计</h2><p>概览经营表现，按维度查看趋势与明细。</p></div>
      <div class="report-controls">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" :disabled="loading" @change="loadAll" />
        <button class="period-button" :disabled="loading" @click="selectDays(7)">近7天</button>
        <button class="period-button" :disabled="loading" @click="selectDays(30)">近30天</button>
        <el-button type="primary" :loading="loading" @click="loadAll">查询</el-button>
      </div>
    </header>

    <div v-if="loading" class="report-state" role="status">正在读取统计数据…</div>
    <div v-else-if="loadError" class="report-state" role="alert"><p>{{ loadError }}</p><el-button @click="loadAll">重新加载</el-button></div>
    <template v-else>
      <div class="summary-grid">
        <section v-for="item in summaries" :key="item.label" :class="['summary-item', { accent: item.accent }]">
          <span class="summary-label">{{ item.label }}</span><strong>{{ item.value }}</strong><span class="summary-hint">{{ item.hint }}</span>
        </section>
      </div>

      <nav class="report-switcher" aria-label="统计维度">
        <button v-for="tab in tabs" :key="tab.key" :aria-pressed="activeReport === tab.key" :class="{ selected: activeReport === tab.key }" @click="selectReport(tab.key)">{{ tab.label }}</button>
      </nav>

      <section v-if="['turnover', 'orders', 'users'].includes(activeReport)" class="report-panel">
        <header class="panel-heading">
          <div><h3>{{ trend.title }}</h3><p>{{ trend.note }}</p></div>
          <button class="detail-toggle" :aria-expanded="showDetails" @click="showDetails = !showDetails">{{ showDetails ? '收起明细' : '查看每日明细' }}</button>
        </header>
        <ReportTrend :title="trend.title" :dates="trend.dates" :series="trend.series" :unit="trend.unit" />
        <div class="chart-footnote">{{ range[0] }} 至 {{ range[1] }} <span>共 {{ trend.dates.length }} 天</span></div>
        <div v-if="showDetails" class="report-details">
          <table>
            <thead><tr><th scope="col">日期</th><th v-for="item in trend.series" :key="item.name" scope="col">{{ item.name }}（{{ trend.unit }}）</th></tr></thead>
            <tbody><tr v-for="row in visibleDetails" :key="row.date"><td>{{ row.date }}</td><td v-for="(value, index) in row.values" :key="index">{{ value }}</td></tr></tbody>
          </table>
          <p v-if="!detailRows.length" class="empty-message">暂无每日明细</p>
          <div v-if="detailRows.length" class="report-pagination"><span>共 {{ detailRows.length }} 条 · {{ detailPage }} / {{ detailPages }} 页</span><button :disabled="detailPage === 1" @click="detailPage--">上一页</button><button :disabled="detailPage >= detailPages" @click="detailPage++">下一页</button></div>
        </div>
      </section>

      <section v-else-if="activeReport === 'sales'" class="report-panel">
        <header class="panel-heading"><div><h3>商品销量 Top 10</h3><p>按已完成订单的商品销量排名，每页展示 5 项。</p></div></header>
        <ol v-if="visibleRanks.length" class="sales-ranking" :start="(rankPage - 1) * 5 + 1">
          <li v-for="item in visibleRanks" :key="item.rank"><span class="rank-index">{{ item.rank }}</span><div class="rank-content"><div class="rank-description"><span :title="item.name">{{ item.name }}</span><strong>{{ item.value }} 件</strong></div><div class="rank-track"><div :style="{ width: barWidth(item.value, top10.max) }"></div></div></div></li>
        </ol>
        <p v-else class="empty-message">所选区间暂无已完成的商品销量记录。</p>
        <div v-if="top10.rows.length" class="report-pagination"><span>共 {{ top10.rows.length }} 项 · {{ rankPage }} / {{ rankPages }} 页</span><button :disabled="rankPage === 1" @click="rankPage--">上一页</button><button :disabled="rankPage >= rankPages" @click="rankPage++">下一页</button></div>
      </section>

      <section v-else class="report-panel">
        <header class="panel-heading"><div><h3>社区拼团概览</h3><p>汇总所选区间的拼团活动、参团人次及订单金额。</p></div></header>
        <div class="group-grid"><div v-for="item in groupCards" :key="item.label"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div></div>
      </section>
    </template>
  </div>
</template>

<script setup>
// 统计报表：/admin/report/turnoverStatistics、ordersStatistics、userStatistics、top10、grouponStatistics（新增）。
// 后端返回的 dateList/turnoverList 等是逗号拼接串（sky 既有口径），本页只做拆分展示，不换算、不补数。
import { computed, onMounted, ref } from 'vue'
import ReportTrend from '../components/ReportTrend.vue'
import { getGrouponStatistics, getOrderReport, getSalesTop10, getTurnoverReport, getUserReport } from '../api'

function lastNDays(n) {
  const end = new Date()
  const begin = new Date(end.getTime() - (n - 1) * 86400000)
  // 用本地时区拼日期串，不用 toISOString（UTC 会把北京时间的凌晨错切成前一天）
  const fmt = (dt) => {
    const y = dt.getFullYear()
    const m = String(dt.getMonth() + 1).padStart(2, '0')
    const d = String(dt.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  return [fmt(begin), fmt(end)]
}

const range = ref(lastNDays(30))
const loading = ref(false)
const loadError = ref('')

function splitList(s) {
  return (s || '').split(',').filter((v) => v !== '')
}

function toNumbers(s) {
  return splitList(s).map(Number)
}

const turnover = ref({ rows: [], values: [], max: 0 })
const orderReport = ref({ rows: [], counts: [], validCounts: [], max: 0, totalOrderCount: '-', validOrderCount: '-', orderCompletionRate: null })
const userReport = ref({ rows: [], values: [], max: 0 })
const top10 = ref({ rows: [], values: [], max: 0 })
const grouponStats = ref({ ongoing: '-', success: '-', failed: '-', expired: '-', memberCount: '-', grouponOrderCount: '-', grouponTurnover: '-' })

const sumTurnover = computed(() =>
  turnover.value.values.reduce((acc, v) => acc + (Number(v) || 0), 0).toFixed(2)
)

function barWidth(value, max) {
  const v = Number(value) || 0
  if (!max || max <= 0) return '0%'
  return Math.max((v / max) * 100, v > 0 ? 2 : 0).toFixed(1) + '%'
}

function rateText(rate) {
  return rate === null || rate === undefined ? '-' : (Number(rate) * 100).toFixed(1) + '%'
}

function toTrend(dates, values) {
  const nums = values
  return {
    rows: dates,
    values: nums,
    max: nums.reduce((m, v) => Math.max(m, v), 0)
  }
}

async function loadAll() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  detailPage.value = 1
  rankPage.value = 1
  const [begin, end] = range.value || []
  try {
    const [t, o, u, s, g] = await Promise.all([
      getTurnoverReport(begin, end),
      getOrderReport(begin, end),
      getUserReport(begin, end),
      getSalesTop10(begin, end),
      getGrouponStatistics(begin, end)
    ])

    const td = t.data || {}
    turnover.value = toTrend(splitList(td.dateList), toNumbers(td.turnoverList))

    const od = o.data || {}
    orderReport.value = {
      ...toTrend(splitList(od.dateList), toNumbers(od.validOrderCountList)),
      validCounts: toNumbers(od.validOrderCountList),
      counts: toNumbers(od.orderCountList),
      totalOrderCount: od.totalOrderCount ?? '-',
      validOrderCount: od.validOrderCount ?? '-',
      orderCompletionRate: od.orderCompletionRate
    }

    const ud = u.data || {}
    userReport.value = toTrend(splitList(ud.dateList), toNumbers(ud.newUserList))

    const sd = s.data || {}
    top10.value = {
      rows: splitList(sd.nameList),
      values: toNumbers(sd.numberList),
      max: 0
    }
    top10.value.max = top10.value.values.reduce((m, v) => Math.max(m, v), 0)

    const gd = g.data || {}
    grouponStats.value = {
      ongoing: gd.ongoing ?? '-',
      success: gd.success ?? '-',
      failed: gd.failed ?? '-',
      expired: gd.expired ?? '-',
      memberCount: gd.memberCount ?? '-',
      grouponOrderCount: gd.grouponOrderCount ?? '-',
      grouponTurnover: gd.grouponTurnover ?? '-'
    }
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    loadError.value = e.message || '报表数据读取失败'
  } finally {
    loading.value = false
  }
}


const activeReport = ref('turnover')
const showDetails = ref(false)
const detailPage = ref(1)
const rankPage = ref(1)
const tabs = [
  { key: 'turnover', label: '营业额趋势' },
  { key: 'orders', label: '订单分析' },
  { key: 'users', label: '用户增长' },
  { key: 'sales', label: '商品销量' },
  { key: 'groupon', label: '社区拼团' }
]
const newUsers = computed(() => userReport.value.values.reduce((sum, value) => sum + Number(value || 0), 0))
const summaries = computed(() => [
  { label: '区间营业额', value: `¥${sumTurnover.value}`, hint: '已完成订单实收', accent: true },
  { label: '下单总数', value: orderReport.value.totalOrderCount, hint: `有效订单 ${orderReport.value.validOrderCount} 单` },
  { label: '订单完成率', value: rateText(orderReport.value.orderCompletionRate), hint: '以有效订单计算' },
  { label: '新增用户', value: newUsers.value, hint: '区间新增注册用户' }
])
const trend = computed(() => {
  if (activeReport.value === 'orders') return {
    title: '订单量与完成情况', note: '比较每日下单总量与有效订单数。', unit: '单', dates: orderReport.value.rows,
    series: [{ name: '下单总数', values: orderReport.value.counts, color: '#7092a6' }, { name: '有效订单', values: orderReport.value.validCounts, color: '#27744d' }]
  }
  if (activeReport.value === 'users') return {
    title: '新增用户趋势', note: '按注册日期统计每日新增用户。', unit: '人', dates: userReport.value.rows,
    series: [{ name: '新增用户', values: userReport.value.values, color: '#27744d' }]
  }
  return {
    title: '营业额走势', note: '按日展示已完成订单的实收金额。', unit: '元', dates: turnover.value.rows,
    series: [{ name: '营业额', values: turnover.value.values, color: '#27744d' }]
  }
})
const detailRows = computed(() => trend.value.dates.map((date, index) => ({ date, values: trend.value.series.map(item => item.values[index] ?? '-') })))
const visibleDetails = computed(() => detailRows.value.slice((detailPage.value - 1) * 7, detailPage.value * 7))
const detailPages = computed(() => Math.max(1, Math.ceil(detailRows.value.length / 7)))
const rankPages = computed(() => Math.max(1, Math.ceil(top10.value.rows.length / 5)))
const visibleRanks = computed(() => top10.value.rows.slice((rankPage.value - 1) * 5, rankPage.value * 5).map((name, index) => {
  const sourceIndex = (rankPage.value - 1) * 5 + index
  return { rank: sourceIndex + 1, name, value: top10.value.values[sourceIndex] }
}))
const groupCards = computed(() => [
  { label: '进行中', value: grouponStats.value.ongoing },
  { label: '已成团', value: grouponStats.value.success },
  { label: '未达成', value: grouponStats.value.failed },
  { label: '已过期', value: grouponStats.value.expired },
  { label: '参团人次', value: grouponStats.value.memberCount },
  { label: '拼团订单', value: grouponStats.value.grouponOrderCount },
  { label: '拼团实收（元）', value: grouponStats.value.grouponTurnover }
])
function selectReport(key) {
  activeReport.value = key
  showDetails.value = false
  detailPage.value = 1
  rankPage.value = 1
}
function selectDays(days) {
  range.value = lastNDays(days)
  loadAll()
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.reports-page { color: #213a2b; }
.report-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 14px; }
.report-heading h2 { margin: 0; font-size: 20px; letter-spacing: -.3px; }
.report-heading p { margin: 5px 0 0; font-size: 12px; color: #718076; }
.report-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.report-controls :deep(.el-date-editor) { width: 250px; flex-grow: 0; }
.period-button, .report-pagination button { background: #fff; border: 1px solid #dfe7e1; border-radius: 6px; color: #52675a; font: inherit; font-size: 12px; padding: 8px 11px; cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .45; }
button:focus-visible { outline: 2px solid #27744d; outline-offset: 3px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-item { display: flex; flex-direction: column; gap: 6px; background: white; border: 1px solid #e3eae5; border-radius: 8px; padding: 14px 18px; }
.summary-item.accent { background: #edf6ef; border-color: #cee1d4; }
.summary-label { font-size: 12px; color: #62796a; }
.summary-item strong { font-size: 25px; line-height: 1.2; font-variant-numeric: tabular-nums; font-weight: 650; }
.summary-item.accent strong { color: #27744d; }
.summary-hint { color: #8a978e; font-size: 11px; }
.report-switcher { display: flex; gap: 6px; border-bottom: 1px solid #dfe7e1; margin-bottom: 14px; }
.report-switcher button { border: 0; border-bottom: 3px solid transparent; background: transparent; padding: 10px 18px; color: #697c70; font: inherit; font-size: 13px; cursor: pointer; white-space: nowrap; }
.report-switcher button.selected { color: #27744d; border-bottom-color: #27744d; font-weight: 600; }
.report-panel { background: #fff; border: 1px solid #e1e9e3; border-radius: 10px; padding: 18px 20px 12px; }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
.panel-heading h3 { margin: 0; font-size: 15px; }
.panel-heading p { margin: 5px 0 0; color: #829087; font-size: 12px; }
.detail-toggle { flex-shrink: 0; border: 1px solid #dfe9e2; border-radius: 6px; background: white; color: #27744d; padding: 7px 11px; font: inherit; font-size: 12px; cursor: pointer; }
.chart-footnote { display: flex; justify-content: space-between; color: #8a988e; font-size: 11px; border-top: 1px solid #edf1ee; padding-top: 10px; }
.report-details { margin-top: 16px; border-top: 1px solid #e7eee9; padding-top: 10px; }
.report-details table { width: 100%; border-collapse: collapse; font-size: 12px; }
.report-details th { text-align: left; color: #6e8275; font-weight: 500; background: #f7faf8; }
.report-details th, .report-details td { padding: 9px 12px; border-bottom: 1px solid #edf2ee; }
.report-details td { font-variant-numeric: tabular-nums; }
.report-pagination { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 12px; font-size: 12px; color: #849389; }
.report-pagination span { margin-right: auto; }
.sales-ranking { list-style: none; margin: 8px 0 0; padding: 0; }
.sales-ranking li { display: flex; align-items: center; gap: 14px; padding: 13px 0; border-bottom: 1px solid #eef3ef; }
.rank-index { color: #7b9986; font-size: 17px; width: 26px; text-align: center; font-variant-numeric: tabular-nums; }
.rank-content { flex: 1; min-width: 0; }
.rank-description { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px; margin-bottom: 9px; }
.rank-description span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-description strong { font-size: 13px; color: #27744d; white-space: nowrap; }
.rank-track { height: 6px; background: #f0f5f1; border-radius: 4px; overflow: hidden; }
.rank-track div { height: 100%; background: #659d79; border-radius: 4px; }
.group-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding: 8px 0; }
.group-grid div { display: flex; flex-direction: column; gap: 10px; padding: 14px 16px; border-left: 2px solid #d8e8dc; background: #f7faf8; }
.group-grid span { font-size: 12px; color: #7b8e81; }
.group-grid strong { font-size: 23px; color: #315c40; font-variant-numeric: tabular-nums; }
.empty-message { color: #88998e; text-align: center; padding: 42px 0; font-size: 13px; }
.report-state { background: #fff; border: 1px solid #e1e9e3; border-radius: 10px; padding: 50px 20px; text-align: center; color: #7c9183; }
@media (max-width: 800px) { .report-heading { gap: 10px; } .summary-item { padding: 12px; } .report-switcher { gap: 0; } .report-switcher button { padding: 10px 12px; } .group-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-height: 650px) { .summary-item { padding-top: 10px; padding-bottom: 10px; gap: 4px; } .summary-item strong { font-size: 22px; } .report-panel { padding-top: 14px; } .report-heading { margin-bottom: 10px; } .summary-grid { margin-bottom: 10px; } }
</style>
