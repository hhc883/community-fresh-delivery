<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">统计报表</h2>
      <el-tag type="info" effect="plain" size="small" class="mode-badge">本页只读</el-tag>
    </div>
    <p class="page-note">数据来自 /admin/report/*，全部由服务端按真实订单聚合；日期区间默认最近 30 天（含今天）。</p>

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
      <el-button type="primary" :loading="loading" @click="loadAll">查询</el-button>
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
        <h3 class="block-title">营业额趋势</h3>
        <p class="block-note">口径：每日已完成订单的实收金额合计；无成交日为 0。区间合计 ¥{{ sumTurnover }}</p>
        <table class="trend-table">
          <thead>
            <tr><th>日期</th><th>营业额（元）</th><th class="bar-head">分布</th></tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in turnover.rows" :key="'t' + i">
              <td>{{ d }}</td>
              <td>{{ turnover.values[i] }}</td>
              <td><div class="bar" :style="{ width: barWidth(turnover.values[i], turnover.max) }"></div></td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 订单趋势 -->
      <section class="block">
        <h3 class="block-title">订单趋势</h3>
        <p class="block-note">
          区间订单总数 {{ orderReport.totalOrderCount }}，有效订单 {{ orderReport.validOrderCount }}，完成率
          {{ rateText(orderReport.orderCompletionRate) }}。口径：有效订单 = 已完成且已支付。
        </p>
        <table class="trend-table">
          <thead>
            <tr><th>日期</th><th>订单数</th><th>有效订单数</th><th class="bar-head">分布（有效订单）</th></tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in orderReport.rows" :key="'o' + i">
              <td>{{ d }}</td>
              <td>{{ orderReport.counts[i] }}</td>
              <td>{{ orderReport.validCounts[i] }}</td>
              <td><div class="bar" :style="{ width: barWidth(orderReport.validCounts[i], orderReport.max) }"></div></td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 新增用户 -->
      <section class="block">
        <h3 class="block-title">新增用户趋势</h3>
        <table class="trend-table">
          <thead>
            <tr><th>日期</th><th>新增用户</th><th class="bar-head">分布</th></tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in userReport.rows" :key="'u' + i">
              <td>{{ d }}</td>
              <td>{{ userReport.values[i] }}</td>
              <td><div class="bar" :style="{ width: barWidth(userReport.values[i], userReport.max) }"></div></td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 商品销量 Top10 -->
      <section class="block">
        <h3 class="block-title">商品销量 Top10</h3>
        <p class="block-note">口径：区间内已完成订单的商品销量合计。</p>
        <table class="trend-table" v-if="top10.rows.length">
          <thead>
            <tr><th>排名</th><th>商品</th><th>销量</th><th class="bar-head">分布</th></tr>
          </thead>
          <tbody>
            <tr v-for="(name, i) in top10.rows" :key="'s' + i">
              <td>{{ i + 1 }}</td>
              <td>{{ name }}</td>
              <td>{{ top10.values[i] }}</td>
              <td><div class="bar" :style="{ width: barWidth(top10.values[i], top10.max) }"></div></td>
            </tr>
          </tbody>
        </table>
        <p v-else class="block-note">区间内没有已完成订单的销量数据。</p>
      </section>

      <!-- 拼团统计 -->
      <section class="block">
        <h3 class="block-title">拼团统计</h3>
        <p class="block-note">
          口径：活动数按 groupon.create_time 落在区间计；拼团订单数/实收金额按订单 order_time 落在区间计（仅已支付订单计入金额）。
        </p>
        <div class="cards">
          <div class="card"><div class="card-label">拼团中</div><div class="card-value">{{ grouponStats.ongoing }}</div></div>
          <div class="card"><div class="card-label">已成团</div><div class="card-value">{{ grouponStats.success }}</div></div>
          <div class="card"><div class="card-label">已失败</div><div class="card-value">{{ grouponStats.failed }}</div></div>
          <div class="card"><div class="card-label">已过期</div><div class="card-value">{{ grouponStats.expired }}</div></div>
          <div class="card"><div class="card-label">参团人次</div><div class="card-value">{{ grouponStats.memberCount }}</div></div>
          <div class="card"><div class="card-label">拼团订单数</div><div class="card-value">{{ grouponStats.grouponOrderCount }}</div></div>
          <div class="card"><div class="card-label">拼团实收（元）</div><div class="card-value">{{ grouponStats.grouponTurnover }}</div></div>
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
  padding: 16px 20px;
  margin-bottom: 16px;
}
.block-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
}
.block-note {
  font-size: 12px;
  color: #909399;
  margin: 0 0 12px;
}
.trend-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.trend-table th,
.trend-table td {
  border-bottom: 1px solid #f0f1f4;
  padding: 6px 8px;
  text-align: left;
  white-space: nowrap;
}
.bar-head {
  width: 50%;
}
.bar {
  height: 10px;
  background: #e6a23c;
  border-radius: 3px;
  min-width: 2px;
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
  color: #909399;
  margin-bottom: 6px;
}
.card-value {
  font-size: 22px;
  font-weight: 600;
}
.state-box {
  padding: 40px;
  text-align: center;
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
}
.state-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
.state-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
}
</style>
