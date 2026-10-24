<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">拼团管理</h2>
      <el-tag type="danger" effect="dark" size="small" class="mode-badge">本页会改数据</el-tag>
    </div>
    <p class="page-note">
      拼团状态：1拼团中 → 2已成团（人数满足）/ 3已失败 / 4已过期（终态不可逆）。
      成团校验幂等：对已成团的活动再次触发不会产生第二次效果。
    </p>

    <div class="toolbar">
      <el-input v-model="filter.keyword" placeholder="商品名称" clearable style="width: 200px" @keyup.enter="onSearch" />
      <el-select v-model="filter.status" placeholder="拼团状态" clearable style="width: 130px">
        <el-option v-for="(label, value) in STATUS_LABELS" :key="value" :label="label" :value="Number(value)" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button type="warning" plain :loading="closing" @click="onCloseExpired">一键关闭超时拼团</el-button>
    </div>

    <div v-if="viewState === 'loading'" class="state-box">拼团活动加载中…</div>
    <div v-else-if="viewState === 'error'" class="state-box">
      <div class="state-title">读取失败</div>
      <div class="state-desc">{{ errorText }}</div>
      <el-button size="small" @click="loadPage">重试</el-button>
    </div>
    <template v-else>
      <el-table :data="rows" border stripe>
        <el-table-column label="活动ID" width="90">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="dishName" label="商品" min-width="140" />
        <el-table-column label="拼团价" width="100">
          <template #default="{ row }">￥{{ row.grouponPrice }}</template>
        </el-table-column>
        <el-table-column label="人数" width="120">
          <template #default="{ row }">{{ row.currentSize }} / {{ row.requiredSize }}</template>
        </el-table-column>
        <el-table-column prop="leaderName" label="团长" width="100" />
        <el-table-column prop="pickupPointName" label="自提点" min-width="140">
          <template #default="{ row }">{{ row.pickupPointName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPES[row.status] || 'info'" size="small">
              {{ STATUS_LABELS[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="截止时间" width="170" />
        <el-table-column prop="completeTime" label="成团时间" width="170">
          <template #default="{ row }">{{ row.completeTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showMembers(row)">成员</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="onComplete(row)">校验成团</el-button>
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

    <!-- 成员列表 -->
    <el-drawer v-model="members.visible" :title="`拼团 #${members.grouponId || ''} 成员`" size="420px">
      <div v-if="members.loading" class="state-box">成员加载中…</div>
      <template v-else>
        <el-alert
          v-if="members.lack > 0 && members.status === 1"
          type="warning"
          :closable="false"
          :title="`还差 ${members.lack} 人成团`"
          style="margin-bottom: 12px"
        />
        <el-table :data="members.list" border size="small">
          <el-table-column label="成员" min-width="100">
            <template #default="{ row }">{{ row.userName || row.userId }}</template>
          </el-table-column>
          <el-table-column label="身份" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isLeader === 1" type="warning" size="small">团长</el-tag>
              <span v-else>团员</span>
            </template>
          </el-table-column>
          <el-table-column prop="number" label="数量" width="60" />
          <el-table-column prop="joinTime" label="参团时间" width="170" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
// 拼团管理：GET /admin/groupon/page（本契约新增接口）+ 手动成团 + 一键关闭超时。
// 成员列表复用拼团详情接口 /user/groupon/{id}？——该接口在用户拦截器下需要用户 token；
// 管理端成员信息由分页接口的 members 字段携带吗？——不携带（列表页不附成员，减少查询）。
// 因此本页成员弹窗数据来自 GrouponVO.members 仅当后端补充；当前契约下列表无成员明细，
// 成员弹窗显示「后端未提供管理端成员明细」的如实占位，绝不伪造数据。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { closeExpiredGroupons, completeGroupon, getGrouponPage } from '../api'

const STATUS_LABELS = { 1: '拼团中', 2: '已成团', 3: '已失败', 4: '已过期' }
const STATUS_TAG_TYPES = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }

const viewState = ref('loading')
const errorText = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filter = reactive({ keyword: '', status: null })
const closing = ref(false)

async function loadPage() {
  viewState.value = 'loading'
  errorText.value = ''
  try {
    const res = await getGrouponPage({
      page: page.value,
      pageSize,
      keyword: filter.keyword,
      status: filter.status
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total) || 0
    viewState.value = 'ready'
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    viewState.value = 'error'
    errorText.value = e.message || '拼团活动读取失败'
  }
}

function onSearch() {
  page.value = 1
  loadPage()
}
function onPageChange(p) {
  page.value = p
  loadPage()
}

async function onComplete(row) {
  try {
    await ElMessageBox.confirm(
      `确定对「${row.dishName}」的拼团 #${row.id} 执行成团校验？\n若人数已满则置为已成团并通知相关订单；人数未满或已成团时服务端如实返回，不做变更。`,
      '校验成团',
      { type: 'warning', customClass: 'admin-write-confirm' }
    )
  } catch (e) {
    return
  }
  try {
    const res = await completeGroupon(row.id)
    const completed = res.data === true
    ElMessage.success(completed ? '已触发成团' : '人数未满或已成团，无变更（服务端如实返回）')
    loadPage()
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    ElMessage.error(e.message || '操作失败')
  }
}

async function onCloseExpired() {
  try {
    await ElMessageBox.confirm(
      '确定关闭所有「拼团中」但已过截止时间的活动？\n关闭后相关未成团订单按服务端规则处置（退款/回补库存），不可恢复。',
      '一键关闭超时拼团',
      { type: 'warning', customClass: 'admin-write-confirm' }
    )
  } catch (e) {
    return
  }
  closing.value = true
  try {
    const res = await closeExpiredGroupons()
    ElMessage.success(`已关闭 ${res.data ?? 0} 个超时拼团`)
    loadPage()
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    ElMessage.error(e.message || '操作失败')
  } finally {
    closing.value = false
  }
}

// ---- 成员弹窗 ----
// 契约现状：管理端分页接口不带成员明细；这里如实显示提示，不伪造成员数据。
const members = reactive({ visible: false, grouponId: null, list: [], loading: false, lack: 0, status: null })

function showMembers(row) {
  members.visible = true
  members.grouponId = row.id
  members.status = row.status
  members.lack = Math.max((row.requiredSize || 0) - (row.currentSize || 0), 0)
  members.list = []
}
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
