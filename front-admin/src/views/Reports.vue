<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">统计报表</h2>
      <el-tag type="success" effect="plain" size="small" class="mode-badge">多维经营分析</el-tag>
    </div>
    <p class="page-note">多维度业务统计图表与数据趋势。支持自定义时间区间分析营业额走势、订单转化、新增顾客及生鲜热销排行。</p>

    <div class="toolbar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="false"
        style="width: 280px"
        @change="loadAll"
      />
      <el-button type="primary" :loading="loading" @click="loadAll">查询统计</el-button>
    </div>

    <div v-if="loading" class="state-box">报表数据读取中…</div>
    <div v-else-if="loadError" class="state-box">
      <div class="state-title">读取失败</div>
      <div class="state-desc">{{ loadError }}</div>
      <el-button size="small" @click="loadAll">重试</el-button>
    </div>
    <template v-else>
      <!-- 营业额趋势 -->
      <section class="block">
        <h3 class="block-title">营业额走势</h3>
        <p class="block-note">每日已完成履约订单的实收金额合计；所选区间累计实收：<strong class="highlight-text">¥{{ sumTurnover }}</strong></p>
        <table class="trend-table">
          <thead>
            <tr><th>日期</th><th>营业额（元）</th><th class="bar-head">走势分布</th></tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in turnover.rows" :key="'t' + i">
              <td>{{ d }}</td>
              <td class="num-cell">￥{{ turnover.values[i] }}</td>
              <td><div class="bar" :style="{ width: barWidth(turnover.values[i], turnover.max) }"></div></td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 订单趋势 -->
      <section class="block">
        <h3 class="block-title">订单量与转化分析</h3>
        <p class="block-note">
          区间下单总数 {{ orderReport.totalOrderCount }} 单，有效履约订单 {{ orderReport.validOrderCount }} 单，全周期订单完成率
          <strong class="highlight-text">{{ rateText(orderReport.orderCompletionRate) }}</strong>。
        </p>
        <table class="trend-table">
          <thead>
            <tr><th>日期</th><th>总下单量</th><th>有效订单数</th><th class="bar-head">有效订单分布</th></tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in orderReport.rows" :key="'o' + i">
              <td>{{ d }}</td>
              <td>{{ orderReport.counts[i] }}</td>
              <td class="num-cell">{{ orderReport.validCounts[i] }}</td>
              <td><div class="bar bar-subtle" :style="{ width: barWidth(orderReport.validCounts[i], orderReport.max) }"></div></td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 新增用户 -->
      <section class="block">
        <h3 class="block-title">新增顾客趋势</h3>
        <p class="block-note">所选时间区间内每日新增注册用户数量。</p>
        <table class="trend-table">
          <thead>
            <tr><th>日期</th><th>新增顾客数</th><th class="bar-head">增长分布</th></tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in userReport.rows" :key="'u' + i">
              <td>{{ d }}</td>
              <td class="num-cell">{{ userReport.values[i] }}</td>
              <td><div class="bar bar-accent" :style="{ width: barWidth(userReport.values[i], userReport.max) }"></div></td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 商品销量 Top10 -->
      <section class="block">
        <h3 class="block-title">生鲜商品销量 Top 10</h3>
        <p class="block-note">区间内已完成履约订单中累计销量前十名的热销生鲜商品。</p>
        <table class="trend-table" v-if="top10.rows.length">
          <thead>
            <tr><th style="width: 60px">排名</th><th>生鲜商品</th><th>累计销量</th><th class="bar-head">销量占比</th></tr>
          </thead>
          <tbody>
            <tr v-for="(name, i) in top10.rows" :key="'s' + i">
              <td>
                <span class="rank-badge" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
              </td>
              <td>{{ name }}</td>
              <td class="num-cell">{{ top10.values[i] }}</td>
              <td><div class="bar" :style="{ width: barWidth(top10.values[i], top10.max) }"></div></td>
            </tr>
          </tbody>
        </table>
        <p v-else class="block-note">所选区间内暂无已完成的商品销量记录。</p>
      </section>

      <!-- 拼团统计 -->
      <section class="block">
        <h3 class="block-title">社区拼团经营汇总</h3>
        <p class="block-note">
          统计区间内发起的拼团活动成团率、顾客参团人次与累计拼团实收金额。
        </p>
        <div class="cards">
          <div class="card"><div class="card-label">进行中拼团</div><div class="card-value">{{ grouponStats.ongoing }}</div></div>
          <div class="card"><div class="card-label">已成功成团</div><div class="card-value text-success">{{ grouponStats.success }}</div></div>
          <div class="card"><div class="card-label">拼团未达成</div><div class="card-value">{{ grouponStats.failed }}</div></div>
          <div class="card"><div class="card-label">超时关闭活动</div><div class="card-value">{{ grouponStats.expired }}</div></div>
          <div class="card"><div class="card-label">累计参团人次</div><div class="card-value">{{ grouponStats.memberCount }}</div></div>
          <div class="card"><div class="card-label">拼团订单总量</div><div class="card-value">{{ grouponStats.grouponOrderCount }}</div></div>
          <div class="card"><div class="card-label">拼团实收金额（元）</div><div class="card-value text-success">￥{{ grouponStats.grouponTurnover }}</div></div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
// 统计报表：/admin/report/turnoverStatistics、ordersStatistics、userStatistics、top10、grouponStatistics（新增）。
// 后端返回的 dateList/turnoverList 等是逗号拼接串（sky 既有口径），本页只做拆分展示，不换算、不补数。
import { computed, onMounted, ref } from 'vue'
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

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin: 16px 0;
}
.block {
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  padding: 18px 22px;
  margin-bottom: 16px;
}
.block-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #1e293b;
}
.block-note {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 14px;
}
.highlight-text {
  color: #27744D;
  font-weight: 600;
}
.trend-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.trend-table th,
.trend-table td {
  border-bottom: 1px solid #f1f5f9;
  padding: 8px 10px;
  text-align: left;
  white-space: nowrap;
}
.trend-table th {
  color: #64748b;
  font-weight: 500;
  background: #f8faf9;
}
.num-cell {
  font-weight: 500;
  color: #1e293b;
}
.bar-head {
  width: 45%;
}
.bar {
  height: 8px;
  background: #27744D;
  border-radius: 4px;
  min-width: 2px;
  transition: width 0.3s ease;
}
.bar-subtle {
  background: #3b82f6;
}
.bar-accent {
  background: #10b981;
}
.rank-badge {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
}
.rank-1 {
  background: #fef3c7;
  color: #d97706;
}
.rank-2 {
  background: #e2e8f0;
  color: #475569;
}
.rank-3 {
  background: #fed7aa;
  color: #c2410c;
}
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.card {
  background: #fafbfc;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  padding: 14px 16px;
}
.card-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}
.card-value {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
}
.text-success {
  color: #27744D;
}
@media (max-width: 1024px) {
  .cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
