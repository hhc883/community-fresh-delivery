<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">大盘</h2>
      <el-tag type="info" effect="plain" size="small" class="mode-badge">本页只读</el-tag>
    </div>
    <p class="page-note">统计窗口：今日（由服务端按 Asia/Shanghai 当天计算，前端不做日期推算）</p>

    <div v-if="viewState === 'loading'" class="state-box">
      <div class="state-title">正在加载</div>
      <div class="state-desc">大盘数据读取中…</div>
    </div>

    <div v-else-if="viewState === 'error'" class="state-box">
      <div class="state-title">读取失败</div>
      <div class="state-desc">{{ errorText }}</div>
      <el-button size="small" @click="load">重试</el-button>
    </div>

    <div v-else class="cards">
      <div class="card">
        <div class="card-label">今日营业额（元）</div>
        <div class="card-value">{{ business.turnover }}</div>
      </div>
      <div class="card">
        <div class="card-label">有效订单数</div>
        <div class="card-value">{{ business.validOrderCount }}</div>
      </div>
      <div class="card">
        <div class="card-label">订单完成率</div>
        <div class="card-value">{{ completionRateText }}</div>
      </div>
      <div class="card">
        <div class="card-label">平均客单价（元）</div>
        <div class="card-value">{{ business.unitPrice }}</div>
      </div>
      <div class="card">
        <div class="card-label">新增用户</div>
        <div class="card-value">{{ business.newUsers }}</div>
      </div>
      <div class="card">
        <div class="card-label">待接单</div>
        <div class="card-value">{{ orders.waitingOrders }}</div>
      </div>
      <div class="card">
        <div class="card-label">派送中</div>
        <div class="card-value">{{ orders.deliveredOrders }}</div>
      </div>
      <div class="card">
        <div class="card-label">已完成订单</div>
        <div class="card-value">{{ orders.completedOrders }}</div>
      </div>
      <div class="card">
        <div class="card-label">商品：起售 / 停售</div>
        <div class="card-value">{{ dishes.sold }} / {{ dishes.discontinued }}</div>
      </div>

      <div class="caliber">
        <p class="caliber-title">口径说明</p>
        <ul>
          <li>本页数据来自 GET /admin/workspace/businessData、overviewOrders、overviewDishes 三个接口，均为服务端实时聚合。</li>
          <li>营业额 = 今日已完成订单的实收金额合计；有效订单 = 已完成且已支付的订单；完成率与客单价由服务端计算，本页不换算。</li>
          <li>订单状态计数口径：待接单（status=2）、已接单（status=3）、派送中（status=4）；取消/待付款订单不计入以上三格。</li>
          <li>需要历史趋势与日期筛选时请使用「统计报表」页。</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
// 只读大盘：GET /admin/workspace/businessData + overviewOrders + overviewDishes（契约见 docs/contracts/api-v1.md）。
// 三个接口都是服务端按「今日」聚合，无日期参数，前端不引入第二处日期真值。
import { computed, onMounted, ref } from 'vue'
import { getBusinessData, getDishOverview, getOrderOverview } from '../api'

const viewState = ref('loading')
const errorText = ref('')

const business = ref({ turnover: '-', validOrderCount: '-', orderCompletionRate: null, unitPrice: '-', newUsers: '-' })
const orders = ref({ waitingOrders: '-', deliveredOrders: '-', completedOrders: '-' })
const dishes = ref({ sold: '-', discontinued: '-' })

const completionRateText = computed(() => {
  const rate = business.value.orderCompletionRate
  return rate === null || rate === undefined ? '-' : (Number(rate) * 100).toFixed(1) + '%'
})

async function load() {
  if (viewState.value === 'loading') return
  viewState.value = 'loading'
  errorText.value = ''
  try {
    // 三个接口互相独立：一个失败不掩盖另外两个的成功（错误信息带出是哪个接口）
    const [b, o, d] = await Promise.allSettled([
      getBusinessData(),
      getOrderOverview(),
      getDishOverview()
    ])
    if (b.status === 'fulfilled' && b.value.data) business.value = b.value.data
    if (o.status === 'fulfilled' && o.value.data) orders.value = o.value.data
    if (d.status === 'fulfilled' && d.value.data) dishes.value = d.value.data
    const failed = [b, o, d].filter((r) => r.status === 'rejected')
    if (failed.length === 3) {
      throw failed[0].reason
    }
    viewState.value = 'ready'
  } catch (err) {
    const e = err || {}
    if (e.code === 401) {
      // 401 由请求层收敛（清 token + 跳 /login），本页只承接一句过渡文案
      viewState.value = 'error'
      errorText.value = '登录状态已失效，正在返回登录页…'
      return
    }
    viewState.value = 'error'
    errorText.value = e.message || '大盘数据读取失败，请稍后重试'
  }
}

onMounted(() => {
  viewState.value = 'idle'
  load()
})
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.card {
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  padding: 18px 20px;
}
.card-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.card-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}
.caliber {
  grid-column: 1 / -1;
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  padding: 16px 20px;
  font-size: 12px;
  color: #606266;
}
.caliber-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #303133;
}
.caliber ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
}
</style>
