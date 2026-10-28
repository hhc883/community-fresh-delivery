<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">订单履约</h2>
      <el-tag type="success" effect="plain" size="small" class="mode-badge">自提与配送</el-tag>
    </div>
    <p class="page-note">
      处理社区生鲜订单，支持接单、派送、提货码核销与状态跟进。
    </p>

    <div class="toolbar">
      <el-input v-model="filter.number" placeholder="订单号" clearable style="width: 180px" @keyup.enter="onSearch" />
      <el-input v-model="filter.phone" placeholder="顾客手机号" clearable style="width: 150px" @keyup.enter="onSearch" />
      <el-select v-model="filter.status" placeholder="订单履约状态" clearable style="width: 140px">
        <el-option v-for="(label, value) in STATUS_LABELS" :key="value" :label="label" :value="Number(value)" />
      </el-select>
      <el-date-picker
        v-model="range"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm:ss"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        style="width: 350px"
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
        <el-table-column prop="number" label="订单编号" min-width="180" />
        <el-table-column prop="userName" label="下单顾客" width="110" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column label="订单金额" width="110">
          <template #default="{ row }">
            <span class="order-price">￥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="履约方式" width="96" align="center">
          <template #default="{ row }">
            <el-tag v-if="isPickupOrder(row)" type="success" size="small" effect="light">门店自提</el-tag>
            <el-tag v-else type="primary" size="small" effect="light">配送到家</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : 'info'" size="small">
              {{ PAY_LABELS[row.payStatus] || '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="履约进度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPES[row.status] || 'info'" size="small">
              {{ STATUS_LABELS[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 2" link type="success" @click="onConfirm(row)">接单</el-button>
            <el-button v-if="row.status === 2" link type="danger" @click="onReject(row)">拒单</el-button>
            <el-button v-if="row.status === 3 && !isPickupOrder(row)" link type="primary" @click="onDelivery(row)">派送</el-button>
            <el-button v-if="row.status === 4 && !isPickupOrder(row)" link type="success" @click="onComplete(row)">完成</el-button>
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

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="detail.visible" title="订单履约明细" size="480px">
      <div v-if="detail.loading" class="state-box">详情加载中…</div>
      <div v-else-if="detail.error" class="state-box">{{ detail.error }}</div>
      <div v-else-if="detail.data" class="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ detail.data.number }}</el-descriptions-item>
          <el-descriptions-item label="履约状态">
            {{ STATUS_LABELS[detail.data.status] || detail.data.status }}
            <el-tag v-if="detail.data.verifyStatus === 1" type="success" size="small" style="margin-left: 6px">已核销提货</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="实收金额">￥{{ detail.data.amount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ PAY_LABELS[detail.data.payStatus] || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.data.orderTime }}</el-descriptions-item>
          <el-descriptions-item label="顾客姓名">{{ detail.data.consignee || detail.data.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.data.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ detail.data.address || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="isPickupOrder(detail.data) && (detail.data.pickupPointName || detail.data.pickupPointId)" label="自提门店">
            {{ detail.data.pickupPointName || ('社区自提点 #' + detail.data.pickupPointId) }}（{{ detail.data.pickupPointAddress || '-' }}）
          </el-descriptions-item>
          <el-descriptions-item v-if="isPickupOrder(detail.data) && detail.data.pickupCode" label="提货凭证码">
            <span class="pickup-code">{{ detail.data.pickupCode }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.data.remark" label="顾客备注">{{ detail.data.remark }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.data.cancelReason" label="取消/退款原因">{{ detail.data.cancelReason }}</el-descriptions-item>
        </el-descriptions>
        <p class="detail-sub">生鲜商品清单</p>
        <el-table :data="detail.data.orderDetailList || []" border size="small">
          <el-table-column prop="name" label="商品名称" min-width="120" />
          <el-table-column prop="number" label="数量" width="70" align="center" />
          <el-table-column label="单价" width="90">
            <template #default="{ row }">￥{{ row.amount }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 提货码核销弹窗 -->
    <el-dialog v-model="verify.visible" title="自提提货码核销" width="420px">
      <el-form label-width="80px">
        <el-form-item label="提货码" required>
          <el-input v-model="verify.pickupCode" maxlength="16" placeholder="请输入顾客出示的 8 位提货码" @keyup.enter="doVerify" />
        </el-form-item>
      </el-form>
      <p class="form-note">输入顾客出示的提货码进行身份核对并完成生鲜商品自提交付。</p>
      <template #footer>
        <el-button @click="verify.visible = false">取消</el-button>
        <el-button type="primary" :loading="verify.saving" @click="doVerify">确认核销</el-button>
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

const STATUS_LABELS = { 1: '待付款', 2: '待接单', 3: '已接单', 4: '派送中', 5: '已完成', 6: '已取消' }
const STATUS_TAG_TYPES = { 1: 'warning', 2: 'warning', 3: 'primary', 4: 'primary', 5: 'success', 6: 'info' }
const PAY_LABELS = { 0: '未支付', 1: '已模拟支付', 2: '已模拟退款' }

const viewState = ref('loading')
const errorText = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filter = reactive({ number: '', phone: '', status: null })
const range = ref(null)

// 履约类型判定：deliveryType 1 配送（绝不显示提货码与自提点），2 自提；缺省时按 pickupPointId 判定
function isPickupOrder(order) {
  if (!order) return false
  if (Number(order.deliveryType) === 1) return false
  if (Number(order.deliveryType) === 2) return true
  return Boolean(order.pickupPointId)
}

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

async function runWrite(action, fn) {
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
  runWrite('接单', () => confirmOrder(row.id))
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
  runWrite('拒单', () => rejectOrder(row.id, reason))
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
  runWrite('取消', () => cancelOrder(row.id, reason))
}

async function onDelivery(row) {
  const ok = await requireConfirmed('派送', `确定开始派送 ${row.number}？\n派送后订单进入「派送中」，完成后由「完成」收尾。`)
  if (!ok) return
  runWrite('派送', () => deliveryOrder(row.id))
}

async function onComplete(row) {
  const ok = await requireConfirmed('完成订单', `确定将 ${row.number} 标记为已完成？\n完成后订单进入终态，计入营业额与销量统计。`)
  if (!ok) return
  runWrite('完成', () => completeOrder(row.id))
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
.order-price {
  font-weight: 600;
  color: #27744D;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.detail-sub {
  font-size: 14px;
  font-weight: 600;
  margin: 18px 0 8px;
  color: #1e293b;
}
.pickup-code {
  font-family: monospace;
  font-size: 15px;
  font-weight: 600;
  color: #27744D;
  background: #e8f5e9;
  padding: 2px 8px;
  border-radius: 4px;
}
.form-note {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
}
</style>
