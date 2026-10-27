<template>
  <div :class="['app-shell', { embedded: route.query.embedded === '1' }]">
    <!-- 顶栏只在登录后出现：登录页自己已有标题，未登录时挂一个「退出登录」是死路 -->
    <header v-if="showChrome" class="app-header">
      <div class="brand">
        <span class="brand-name">生鲜运营端</span>
        <el-tag type="warning" effect="dark" size="small">运营端</el-tag>
      </div>
      <nav class="nav">
        <router-link to="/overview" class="nav-link" :class="{ active: isActive('/overview') }">大盘</router-link>
        <router-link to="/dishes" class="nav-link" :class="{ active: isActive('/dishes') }"><span class="nav-dot" title="本页会改数据"></span>商品管理</router-link>
        <router-link to="/orders" class="nav-link" :class="{ active: isActive('/orders') }"><span class="nav-dot" title="本页会改数据"></span>订单履约</router-link>
        <router-link to="/groupon" class="nav-link" :class="{ active: isActive('/groupon') }"><span class="nav-dot" title="本页会改数据"></span>拼团管理</router-link>
        <router-link to="/reports" class="nav-link" :class="{ active: isActive('/reports') }">统计报表</router-link>
        <span class="nav-legend" title="橙点入口对应的页面会修改数据，操作前有二次确认">● 会改数据（操作前有二次确认）</span>
      </nav>
      <div class="actions">
        <el-button size="small" @click="onLogout">退出登录</el-button>
      </div>
    </header>

    <main class="app-main" :class="{ 'with-header': showChrome }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 外壳：右上角标明当前是「运营端」，并给退出登录。
// 退出 = 调 /admin/employee/logout（尽力通知）+ 清 admin_token + 回 /login，
// 清态与跳转都走 api 层的 logout()，本组件不各自读 storage（键名真源只有 TOKEN_STORAGE_KEY 一处）。
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { logout } from './api'

const route = useRoute()
const showChrome = computed(() => route.path !== '/login' && route.query.embedded !== '1')
const isActive = (path) => route.path === path
const onLogout = () => logout()
</script>

<style>
:root {
  --admin-bg: #f5f6f8;
  --admin-border: #e6e7eb;
}
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  background: var(--admin-bg);
  font-family: 'Helvetica Neue', Helvetica, Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #303133;
}
.app-shell {
  min-height: 100vh;
}
.app-header {
  display: flex;
  align-items: center;
  gap: 24px;
  /* F-20.22 顶栏从两条变六条：定高 56px 会在窄屏把导航挤出去，改成 min-height + 可换行，
     宽屏读数与改动前一致（单行 56px），窄屏则整条 header 自然加高。 */
  min-height: 56px;
  padding: 8px 24px;
  flex-wrap: wrap;
  background: #fff;
  border-bottom: 1px solid var(--admin-border);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.brand-name {
  font-size: 16px;
  font-weight: 600;
}
.nav {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  flex: 1;
  min-width: 0;
}
.nav-link {
  color: #606266;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 2px;
  border-bottom: 2px solid transparent;
}
.nav-link.active {
  color: #303133;
  font-weight: 600;
  border-bottom-color: #e6a23c;
}
.actions {
  margin-left: auto;
}
.app-main {
  padding: 24px;
}
.app-main.with-header {
  padding-top: 24px;
}
.page {
  max-width: 1280px;
  margin: 0 auto;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
/* FE-G6-10（读写可辨）：页眉行（标题 + 读/写徽标）。徽标本体是 el-tag，这里只管排布；
   写页挂 danger「本页会改数据」、只读页挂 info「本页只读」，常驻 ready 态（原先这类话只在出错态出现）。 */
.page-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
/* 写侧入口的橙点与图例：六个入口平铺，运营不看说明文就读不出「点了就改库」，
   四条写侧链接挂橙点 + 一条常驻图例，与各页页眉的「本页会改数据」互相印证。 */
.nav-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e6a23c;
  margin-right: 4px;
  vertical-align: middle;
}
.nav-legend {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  margin-left: 4px;
}
.page-note {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
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
}
/* F-20.22 四个 P0 页写动作的二次确认框（ElMessageBox 挂在 body 下，页面 scoped 样式作用不到，
   所以这一条只有全局一份；四个页面 customClass 传的是同一个串 'admin-write-confirm'）。
   为什么要改 white-space：确认文案是「把后果写全」的多行清单（核销/取消/删除/发布门店各自的副作用），
   默认渲染会把 \n 折成空格、糊成一小块 420px 的单行——运营读不到重点，纪律 2 就只剩形式。
   这里让换行真的换行、并把框加宽到能读完一行。 */
.admin-write-confirm {
  width: 640px;
  max-width: 92vw;
}
.admin-write-confirm .el-message-box__message {
  white-space: pre-line;
  line-height: 1.75;
  font-size: 13px;
  color: #303133;
}
</style>
<style>
.embedded .app-main {
  padding: 16px 20px;
}
.embedded .page {
  max-width: none;
}
.embedded .el-button--primary {
  --el-button-bg-color: #ffc200;
  --el-button-border-color: #ffc200;
  --el-button-text-color: #333333;
  --el-button-hover-bg-color: #ffd033;
  --el-button-hover-border-color: #ffd033;
  --el-button-hover-text-color: #333333;
  font-weight: 500;
}
.embedded .el-tag {
  border-radius: 4px;
}
/* 嵌入苍穹外卖管理端主视觉：增强表格、筛选栏与卡片层次 */
.embedded .el-table {
  border-radius: 6px;
  overflow: hidden;
  font-size: 13px;
}
.embedded .el-table th.el-table__cell {
  background-color: #f8f9fb !important;
  color: #303133;
  font-weight: 600;
  padding: 10px 0;
}
.embedded .el-table td.el-table__cell {
  padding: 10px 0;
}
.embedded .toolbar {
  background: #ffffff;
  padding: 14px 16px;
  border-radius: 6px;
  border: 1px solid var(--admin-border);
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.embedded .card,
.embedded .block {
  border-radius: 6px;
  border: 1px solid var(--admin-border);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
}
.embedded .el-dialog {
  border-radius: 8px;
  overflow: hidden;
}
.embedded .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #ffc200 inset !important;
}
.embedded .el-pagination.is-background .el-pager li.is-active {
  background-color: #ffc200 !important;
  color: #333333 !important;
  font-weight: 600;
}
</style>
