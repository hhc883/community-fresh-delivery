<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">订单履约</h2>
      <el-tag type="danger" effect="dark" size="small" class="mode-badge">本页会改数据</el-tag>
    </div>
    <p class="page-note">
      订单列表、详情与状态流转均为真实数据库读写。状态机：1待付款 → 2待接单 → 3已接单 → 4派送中 → 5已完成；6已取消。
      自提订单可用提货码核销（已核销订单不可二次核销，由服务端判定）。
    </p>

    <div class="toolbar">
      <el-input v-model="filter.number" placeholder="订单号" clearable style="width: 180px" @keyup.enter="onSearch" />
      <el-input v-model="filter.phone" placeholder="手机号" clearable style="width: 150px" @keyup.enter="onSearch" />
      <el-select v-model="filter.status" placeholder="订单状态" clearable style="width: 130px">
        <el-option v-for="(label, value) in STATUS_LABELS" :key="value" :label="label" :value="Number(value)" />
      </el-select>
      <el-date-picker
        v-model="range"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm:ss"
        start-placeholder="下单开始时间"
        end-placeholder="下单结束时间"
        style="width: 360px"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" plain @click="openVerify">提货码核销</el-button>
    </div>

    <div v-if="viewState === 'loading'" class="state-box">订单列表加载中…</div>
    <div v-else-if="viewState === 'error'" class="state-box">
      <div class="state-title">读取失败</div>
      <div class="state-desc">{{ errorText }}</div>
      <el-button size="small" @click="loadOrders">重试</el-button>
    </div>
    <template v-else>
      <el-table :data="rows" border stripe>
        <el-table-column prop="number" label="订单号" min-width="180" />
        <el-table-column prop="userName" label="下单用户" width="100" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">￥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="方式" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.pickupPointName || row.pickupCode" type="success" size="small">自提</el-tag>
            <el-tag v-else type="primary" size="small">配送</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付" width="80">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : 'info'" size="small">
              {{ PAY_LABELS[row.payStatus] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPES[row.status] || 'info'" size="small">
              {{ STATUS_LABELS[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 2" link type="success" @click="onConfirm(row)">接单</el-button>
            <el-button v-if="row.status === 2" link type="danger" @click="onReject(row)">拒单</el-button>
            <el-button v-if="row.status === 3" link type="primary" @click="onDelivery(row)">派送</el-button>
            <el-button v-if="row.status === 4 && !(row.pickupPointName || row.pickupCode)" link type="success" @click="onComplete(row)">完成</el-button>
            <el-button v-if="row.status === 2 || row.status === 3" link type="warning" @click="onCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
      />
    </template>

    <!-- 订单详情 -->
    <el-drawer v-model="detail.visible" title="订单详情" size="480px">
      <div v-if="detail.loading" class="state-box">详情加载中…</div>
      <div v-else-if="detail.error" class="state-box">{{ detail.error }}</div>
      <div v-else-if="detail.data" class="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ detail.data.number }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ STATUS_LABELS[detail.data.status] || detail.data.status }}
            <el-tag v-if="detail.data.verifyStatus === 1" type="success" size="small" style="margin-left: 6px">已核销</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="实收金额">￥{{ detail.data.amount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ PAY_LABELS[detail.data.payStatus] || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.data.orderTime }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.data.consignee || detail.data.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.data.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ detail.data.address || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.data.pickupPointName" label="自提点">
            {{ detail.data.pickupPointName }}（{{ detail.data.pickupPointAddress || '-' }}）
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.data.pickupCode" label="提货码">
            <span class="pickup-code">{{ detail.data.pickupCode }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.data.remark" label="备注">{{ detail.data.remark }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.data.cancelReason" label="取消原因">{{ detail.data.cancelReason }}</el-descriptions-item>
        </el-descriptions>
        <p class="detail-sub">商品明细</p>
        <el-table :data="detail.data.orderDetailList || []" border size="small">
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column prop="number" label="数量" width="60" />
          <el-table-column label="单价" width="80">
            <template #default="{ row }">￥{{ row.amount }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 提货码核销 -->
    <el-dialog v-model="verify.visible" title="提货码核销" width="420px">
      <el-form label-width="80px">
        <el-form-item label="提货码" required>
          <el-input v-model="verify.pickupCode" maxlength="16" placeholder="用户出示的 8 位提货码" @keyup.enter="doVerify" />
        </el-form-item>
      </el-form>
      <p class="form-note">核销成功后订单标记为已核销；同一提货码第二次核销会被服务端拒绝，不产生第二次履约。</p>
      <template #footer>
        <el-button @click="verify.visible = false">取消</el-button>
        <el-button type="primary" :loading="verify.saving" @click="doVerify">核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 订单履约台：GET /admin/order/conditionSearch + 详情 + 状态流转 + 提货码核销。
// 状态与允许动作的对应关系见 docs/contracts/state-machine.md：
//   2待接单 → 接单/拒单/取消；3已接单 → 派送/取消；4派送中 → 完成（配送单）；
//   自提单（有 pickupCode/pickupPointName）走提货码核销，不出现「完成」按钮。
// 所有动作成功后重拉当前页核对，不本地改行。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelOrder, completeOrder, confirmOrder, deliveryOrder, getOrderDetail, getOrderPage,
  rejectOrder, verifyOrder
} from '../api'

const STATUS_LABELS = { 1: '待付款', 2: '待接单', 3: '已接单', 4: '派送中', 5: '已完成', 6: '已取消', 7: '已退款' }
const STATUS_TAG_TYPES = { 1: 'warning', 2: 'warning', 3: 'primary', 4: 'primary', 5: 'success', 6: 'info', 7: 'danger' }
const PAY_LABELS = { 0: '未支付', 1: '已支付', 2: '已退款' }

const viewState = ref('loading')
const errorText = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filter = reactive({ number: '', phone: '', status: null })
const range = ref(null)

async function loadOrders() {
  viewState.value = 'loading'
  errorText.value = ''
  try {
    const res = await getOrderPage({
      page: page.value,
      pageSize,
      number: filter.number,
      phone: filter.phone,
      status: filter.status,
      beginTime: range.value && range.value[0],
      endTime: range.value && range.value[1]
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total) || 0
    viewState.value = 'ready'
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    viewState.value = 'error'
    errorText.value = e.message || '订单列表读取失败'
  }
}

function onSearch() {
  page.value = 1
  loadOrders()
}
function onReset() {
  filter.number = ''
  filter.phone = ''
  filter.status = null
  range.value = null
  page.value = 1
  loadOrders()
}
function onPageChange(p) {
  page.value = p
  loadOrders()
}

function requireConfirmed(action, text) {
  return ElMessageBox.confirm(text, action, {
    type: 'warning',
    customClass: 'admin-write-confirm'
  }).catch(() => null)
}

async function runWrite(action, fn, row) {
  try {
    await fn()
    ElMessage.success('操作成功')
    await loadOrders()
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    ElMessage.error(e.message || (action + '失败'))
  }
}

async function onConfirm(row) {
  const ok = await requireConfirmed('接单', `确定接单 ${row.number}？\n接单后订单进入「已接单」，可继续派送。`)
  if (!ok) return
  runWrite('接单', () => confirmOrder(row.id), row)
}

async function onReject(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入拒单原因（会通知用户）', '拒单', {
      inputPattern: /.+/,
      inputErrorMessage: '拒单原因不能为空',
      customClass: 'admin-write-confirm'
    })
    reason = value
  } catch (e) {
    return
  }
  runWrite('拒单', () => rejectOrder(row.id, reason), row)
}

async function onCancel(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      inputPattern: /.+/,
      inputErrorMessage: '取消原因不能为空',
      customClass: 'admin-write-confirm'
    })
    reason = value
  } catch (e) {
    return
  }
  runWrite('取消', () => cancelOrder(row.id, reason), row)
}

async function onDelivery(row) {
  const ok = await requireConfirmed('派送', `确定开始派送 ${row.number}？\n派送后订单进入「派送中」，完成后由「完成」收尾。`)
  if (!ok) return
  runWrite('派送', () => deliveryOrder(row.id), row)
}

async function onComplete(row) {
  const ok = await requireConfirmed('完成订单', `确定将 ${row.number} 标记为已完成？\n完成后订单进入终态，计入营业额与销量统计。`)
  if (!ok) return
  runWrite('完成', () => completeOrder(row.id), row)
}

// ---- 详情 ----
const detail = reactive({ visible: false, loading: false, error: '', data: null })

async function openDetail(row) {
  detail.visible = true
  detail.loading = true
  detail.error = ''
  detail.data = null
  try {
    const res = await getOrderDetail(row.id)
    detail.data = res.data || {}
    detail.loading = false
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    detail.loading = false
    detail.error = e.message || '订单详情读取失败'
  }
}

// ---- 提货码核销 ----
const verify = reactive({ visible: false, pickupCode: '', saving: false })

function openVerify() {
  verify.pickupCode = ''
  verify.visible = true
}

async function doVerify() {
  const code = verify.pickupCode.trim()
  if (!code) return ElMessage.warning('请输入提货码')
  verify.saving = true
  try {
    const res = await verifyOrder({ pickupCode: code })
    const order = res.data || {}
    verify.visible = false
    ElMessage.success(`核销成功：订单 ${order.number || code} 已完成履约`)
    loadOrders()
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    // 服务端拒绝（码不存在 / 已核销 / 状态不允许）原样透出，绝不本地造成功
    ElMessage.error(e.message || '核销失败')
  } finally {
    verify.saving = false
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0 14px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.detail-sub {
  font-weight: 600;
  font-size: 13px;
  margin: 16px 0 8px;
}
.pickup-code {
  font-family: monospace;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
}
.form-note {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px;
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
