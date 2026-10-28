<template>
  <div :class="['app-shell', { embedded: route.query.embedded === '1' }]">
    <!-- 独立运行时展示顶栏：嵌入模式下由外壳统一接管顶栏 -->
    <header v-if="showChrome" class="app-header">
      <div class="brand">
        <span class="brand-name">社区鲜选</span>
        <el-tag type="success" effect="dark" size="small">生鲜运营端</el-tag>
      </div>
      <nav class="nav">
        <router-link to="/overview" class="nav-link" :class="{ active: isActive('/overview') }">工作台大盘</router-link>
        <router-link to="/dishes" class="nav-link" :class="{ active: isActive('/dishes') }">商品管理</router-link>
        <router-link to="/orders" class="nav-link" :class="{ active: isActive('/orders') }">订单履约</router-link>
        <router-link to="/groupon" class="nav-link" :class="{ active: isActive('/groupon') }">拼团管理</router-link>
        <router-link to="/reports" class="nav-link" :class="{ active: isActive('/reports') }">统计报表</router-link>
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
  --admin-bg: #f4f6f8;
  --admin-border: #e2e8f0;
  --fresh-green: #27744D;
  --fresh-green-hover: #318a5d;
  --fresh-green-light: #e8f5e9;
  --el-color-primary: #27744d;
  --el-color-primary-light-3: #689e80;
  --el-color-primary-light-5: #94baa6;
  --el-color-primary-light-7: #bfd5c9;
  --el-color-primary-light-8: #d4e3db;
  --el-color-primary-light-9: #e9f1ed;
  --el-color-primary-dark-2: #1f5d3e;
}
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  background: var(--admin-bg);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #1a202c;
}
.app-shell {
  min-height: 100vh;
}
.app-header {
  display: flex;
  align-items: center;
  gap: 24px;
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
  color: #1a202c;
}
.nav {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  flex: 1;
  min-width: 0;
}
.nav-link {
  color: #64748b;
  text-decoration: none;
  font-size: 14px;
  padding: 6px 4px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}
.nav-link:hover {
  color: var(--fresh-green);
}
.nav-link.active {
  color: var(--fresh-green);
  font-weight: 600;
  border-bottom-color: var(--fresh-green);
}
.actions {
  margin-left: auto;
}
.app-main {
  padding: 20px 24px;
}
.app-main.with-header {
  padding-top: 20px;
}
.page {
  max-width: 1400px;
  margin: 0 auto;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #1a202c;
}
.page-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.page-note {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 16px;
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
  color: #1a202c;
}
.state-desc {
  font-size: 13px;
  color: #64748b;
}

/* 操作二次确认框：换行支持与清晰排版 */
.admin-write-confirm {
  width: 600px;
  max-width: 92vw;
  border-radius: 8px;
}
.admin-write-confirm .el-message-box__message {
  white-space: pre-line;
  line-height: 1.75;
  font-size: 13px;
  color: #334155;
}
.admin-write-confirm .el-button--primary {
  background-color: var(--fresh-green) !important;
  border-color: var(--fresh-green) !important;
  color: #fff !important;
}
</style>

<style>
/* 嵌入外壳管理端的主题统配 */
.embedded .app-main {
  padding: 16px 20px;
}
.embedded .page {
  max-width: none;
}

/* 主按钮统一定义为鲜选生鲜绿 */
.embedded .el-button--primary:not(.is-link):not(.is-text):not(.is-plain),
.el-button--primary:not(.is-link):not(.is-text):not(.is-plain) {
  --el-button-bg-color: #27744D;
  --el-button-border-color: #27744D;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #318a5d;
  --el-button-hover-border-color: #318a5d;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #1f5f3e;
  --el-button-active-border-color: #1f5f3e;
  font-weight: 500;
}

.el-button--primary.is-link,
.el-button--primary.is-text {
  --el-button-text-color: #27744d;
  --el-button-hover-text-color: #1f5d3e;
  --el-button-active-text-color: #1f5d3e;
}

.el-button--primary.is-plain {
  --el-button-text-color: #27744d;
  --el-button-bg-color: #e9f1ed;
  --el-button-border-color: #bfd5c9;
  --el-button-hover-bg-color: #27744d;
  --el-button-hover-border-color: #27744d;
  --el-button-hover-text-color: #ffffff;
}

.embedded .el-tag {
  border-radius: 4px;
}

/* 增强表格、筛选栏与卡片层次 */
.embedded .el-table,
.el-table {
  border-radius: 6px;
  overflow: hidden;
  font-size: 13px;
}
.embedded .el-table th.el-table__cell,
.el-table th.el-table__cell {
  background-color: #f8faf9 !important;
  color: #1e293b;
  font-weight: 600;
  padding: 10px 0;
  border-bottom: 1px solid var(--admin-border);
}
.embedded .el-table td.el-table__cell,
.el-table td.el-table__cell {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}
.embedded .toolbar,
.toolbar {
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
.embedded .block,
.card,
.block {
  border-radius: 8px;
  border: 1px solid var(--admin-border);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}
.embedded .el-dialog,
.el-dialog {
  border-radius: 8px;
  overflow: hidden;
}
.embedded .el-input__wrapper.is-focus,
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #27744D inset !important;
}
.embedded .el-pagination.is-background .el-pager li.is-active,
.el-pagination.is-background .el-pager li.is-active {
  background-color: #27744D !important;
  color: #ffffff !important;
  font-weight: 600;
}
</style>
